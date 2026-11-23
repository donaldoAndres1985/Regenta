package com.regenta.facturacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.facturacion.domain.Certificado;
import com.regenta.facturacion.domain.EstadoCertificado;
import com.regenta.facturacion.domain.EstadoFactura;
import com.regenta.facturacion.domain.EventoDeTransmision;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.infrastructure.BovedaCifrada;
import com.regenta.facturacion.infrastructure.CertificadoRepositorio;
import com.regenta.facturacion.infrastructure.ClienteDeLaDianSoap;
import com.regenta.facturacion.infrastructure.DianSimulada;
import com.regenta.facturacion.infrastructure.FacturaRepositorio;
import com.regenta.facturacion.infrastructure.FirmadorXadesEpes;
import com.regenta.facturacion.infrastructure.TransmisionesLog;
import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-117 de punta a punta, con la DIAN simulada por contrato: la factura emitida se firma (UBL completo,
 * CUFE oficial, XAdES-EPES), viaja en un ZIP por SOAP con WS-Security y se interpreta la respuesta.
 */
class TransmisionALaDianTest {

    private final UUID negocio = UUID.randomUUID();
    private final FacturaRepositorio facturas = mock(FacturaRepositorio.class);
    private final CertificadoRepositorio certificados = mock(CertificadoRepositorio.class);
    private final TransmisionesLog transmisiones = mock(TransmisionesLog.class);
    private final AlmacenDeDocumentos almacen = mock(AlmacenDeDocumentos.class);
    private final GestionDeContingencia contingencia = mock(GestionDeContingencia.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final DocumentosAFirmar documentos = mock(DocumentosAFirmar.class);
    private final BovedaCifrada boveda = new BovedaCifrada(new AlmacenEnMemoria(), llave());
    private final AtomicReference<byte[]> xmlGuardado = new AtomicReference<>();
    private DianSimulada dian;
    private GestionDeFirmaYTransmision gestion;
    private Factura factura;
    private CertificadoDeFirma certificado;

    @BeforeEach
    void preparar() throws Exception {
        dian = new DianSimulada();
        ParametrosDeLaDian parametros = FacturaDePrueba.parametros(dian.url(), "2", "");
        gestion = new GestionDeFirmaYTransmision(facturas, certificados, transmisiones,
                new FirmadorXadesEpes(parametros), almacen, new ClienteDeLaDianSoap(parametros, 2, 1, 0), boveda,
                contingencia, eventos, documentos);
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"),
                Set.of("FACTURACION"), Set.of(), Set.of()));

        byte[] p12 = CertificadoDePrueba.vigente("clave");
        certificado = CertificadoDeFirma.abrir(p12, "clave");
        String referencia = boveda.guardar(negocio, "certificado", CertificadoDeFirma.sobre(p12, "clave"));
        Certificado c = Certificado.registrar(negocio, "principal", "x", "1", LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1), referencia);
        when(certificados.findByNegocioIdAndEstado(negocio, EstadoCertificado.ACTIVO)).thenReturn(List.of(c));

        DocumentoAFirmar doc = FacturaDePrueba.documento(negocio);
        factura = doc.factura();
        when(documentos.de(factura)).thenReturn(doc);
        when(documentos.nombreDeArchivo(factura)).thenReturn("0900373076000260000007b");
        when(facturas.findByIdAndNegocioId(factura.getId(), negocio)).thenReturn(Optional.of(factura));
        when(almacen.guardar(eq(negocio), anyString(), any(), anyString())).thenAnswer(i -> {
            xmlGuardado.set(i.getArgument(2));
            return "s3://regenta/" + i.getArgument(1);
        });
        when(almacen.leer(eq(negocio), anyString())).thenAnswer(i -> xmlGuardado.get());
    }

    @AfterEach
    void limpiar() {
        dian.close();
        ContextoDeNegocio.limpiar();
    }

    @Test
    void c1_aceptadaQuedaGuardadoElCufeQueDevuelveLaDian() {
        gestion.firmar(factura.getId());
        String cufeCalculado = factura.getCufe();
        dian.luego(DianSimulada.aceptada(cufeCalculado, "Regla: FAJ41, Notificación: nombre del municipio"));

        ResultadoDeTransmision r = gestion.transmitir(factura.getId());

        assertThat(r.aceptada()).isTrue();
        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.ACEPTADA);
        assertThat(factura.getCufe()).isEqualTo(cufeCalculado);
        assertThat(factura.getRespuestaDian()).containsEntry("cufe", cufeCalculado).containsKey("notificaciones");
        // Lo que llegó a la DIAN: el XML firmado que se guardó, con firma válida, en el ZIP con su nombre.
        DianSimulada.Pedido pedido = dian.pedidos.get(0);
        assertThat(pedido.firmaValida()).isTrue();
        assertThat(pedido.archivoEnElZip()).isEqualTo("fv0900373076000260000007b.xml");
        assertThat(FirmadorXadesEpes.verificar(pedido.xml(), certificado.certificado())).isTrue();
        verify(eventos).registrar(eq(negocio), eq("factura"), eq(factura.getId()), eq("factura_aceptada"), any());
        verify(transmisiones).registrar(eq(negocio), eq(factura.getId()), eq(EventoDeTransmision.ENVIO), isNull(),
                anyString(), anyString(), eq(200), isNull(), anyString(), any());
    }

    @Test
    void c1_siLaDianDevuelveOtroCufeMandaElDeLaDian() {
        gestion.firmar(factura.getId());
        String calculado = factura.getCufe();
        dian.luego(DianSimulada.aceptada("f".repeat(96)));

        gestion.transmitir(factura.getId());

        assertThat(factura.getCufe()).isEqualTo("f".repeat(96));
        assertThat(factura.getRespuestaDian()).containsEntry("cufe_calculado", calculado);
    }

    @Test
    void c2_c4_rechazadaQuedaElCodigoYUnMensajeQueNombraElCampo() {
        gestion.firmar(factura.getId());
        dian.luego(DianSimulada.rechazada("Regla: FAK21, Rechazo: ID de adquiriente no Informado"));

        ResultadoDeTransmision r = gestion.transmitir(factura.getId());

        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.RECHAZADA);
        assertThat(r.codigoError()).isEqualTo("FAK21");
        assertThat(factura.getRespuestaDian()).containsEntry("codigo", "FAK21");
        assertThat(factura.getRespuestaDian().get("mensaje").toString())
                .startsWith("Adquiriente · número de documento (datos del cliente)");
        // Visible para quien la emitió: el detalle de la factura y el aviso de Alertas llevan el motivo.
        assertThat(FacturaDetalle.de(factura, List.of(), List.of(), List.of()).mensajeRechazo())
                .contains("número de documento");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> payload = ArgumentCaptor.forClass(java.util.Map.class);
        verify(eventos).registrar(eq(negocio), eq("factura"), eq(factura.getId()), eq("factura_rechazada"),
                payload.capture());
        assertThat(payload.getValue()).containsEntry("codigo_error", "FAK21");
        assertThat(payload.getValue().get("mensaje").toString()).contains("ID de adquiriente no Informado");
    }

    @Test
    void c3_laDianSinResponderTerminaEnContingencia() {
        gestion.firmar(factura.getId());
        for (int i = 0; i < 3; i++) {
            dian.luego(null);   // HTTP 500 sin mensaje SOAP
        }

        gestion.transmitir(factura.getId());
        gestion.transmitir(factura.getId());
        verify(contingencia, never()).abrirSiHaceFalta(anyString());
        gestion.transmitir(factura.getId());

        assertThat(factura.getEstado()).isEqualTo(EstadoFactura.ENVIADA);
        assertThat(factura.getIntentosEnvio()).isEqualTo((short) 3);
        verify(contingencia, times(1)).abrirSiHaceFalta(anyString());
        verify(transmisiones, times(3)).registrar(eq(negocio), eq(factura.getId()), eq(EventoDeTransmision.ENVIO),
                isNull(), isNull(), isNull(), isNull(), eq("SIN_RESPUESTA"), anyString(), isNull());
    }

    private static String llave() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }
}
