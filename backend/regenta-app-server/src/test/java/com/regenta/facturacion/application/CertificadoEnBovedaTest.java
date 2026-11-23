package com.regenta.facturacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.facturacion.domain.Certificado;
import com.regenta.facturacion.domain.EstadoCertificado;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.infrastructure.BovedaCifrada;
import com.regenta.facturacion.infrastructure.CertificadoRepositorio;
import com.regenta.facturacion.infrastructure.FacturaRepositorio;
import com.regenta.facturacion.infrastructure.FirmadorXadesEpes;
import com.regenta.facturacion.infrastructure.TransmisionesLog;
import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-116 de punta a punta, con dos negocios y un certificado de prueba: el alta lo deja cifrado en la
 * bóveda (C1), la firma valida (C2), uno vencido no entra (C4) y un negocio no firma con lo de otro (C5).
 */
class CertificadoEnBovedaTest {

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final CertificadoRepositorio certificados = mock(CertificadoRepositorio.class);
    private final FacturaRepositorio facturas = mock(FacturaRepositorio.class);
    private final AlmacenDeDocumentos documentos = mock(AlmacenDeDocumentos.class);
    private final BovedaCifrada boveda = new BovedaCifrada(new AlmacenEnMemoria(), llave());
    private final GestionDeFirmaYTransmision firma = new GestionDeFirmaYTransmision(facturas, certificados,
            mock(TransmisionesLog.class), new FirmadorXadesEpes(), documentos, mock(ClienteDeLaDian.class), boveda,
            mock(GestionDeContingencia.class), mock(RegistroDeEventos.class));
    private final List<Certificado> guardados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    void c1_laBaseSoloGuardaLaReferenciaYLaFirmaValida() {
        byte[] p12 = CertificadoDePrueba.vigente("clave-A");
        Certificado c = registrar(negocioA, "principal", p12, "clave-A");

        assertThat(c.getReferenciaKms()).doesNotContain(Base64.getEncoder().encodeToString(p12).substring(0, 40))
                .doesNotContain("clave-A").contains("boveda");
        assertThat(c.getEmisor()).contains("Negocio de Prueba");

        Factura factura = factura(negocioA);
        ArgumentCaptor<byte[]> xml = ArgumentCaptor.forClass(byte[].class);
        when(documentos.guardar(eq(negocioA), anyString(), xml.capture(), anyString())).thenReturn("s3://xml");
        firma.firmar(factura.getId());

        CertificadoDeFirma abierto = CertificadoDeFirma.abrir(p12, "clave-A");
        assertThat(FirmadorXadesEpes.verificar(xml.getValue(), abierto.certificado())).isTrue();
        verify(factura).firmar(anyString(), eq("s3://xml"));
    }

    @Test
    void c4_unCertificadoVencidoNoSeCarga() {
        byte[] vencido = CertificadoDePrueba.p12("x", LocalDate.now().minusYears(2), LocalDate.now().minusDays(1));
        en(negocioA);

        assertThatThrownBy(() -> firma.registrarCertificado(new SolicitudDeCertificado("viejo",
                Base64.getEncoder().encodeToString(vencido), "x")))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("venció");
        verify(certificados, never()).save(any());
    }

    @Test
    void conLaClaveEquivocadaNoSeCarga() {
        en(negocioA);

        assertThatThrownBy(() -> firma.registrarCertificado(new SolicitudDeCertificado("p",
                Base64.getEncoder().encodeToString(CertificadoDePrueba.vigente("buena")), "mala")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void c5_elNegocioBNoFirmaConElCertificadoDeA() {
        Certificado deA = registrar(negocioA, "principal", CertificadoDePrueba.vigente("clave-A"), "clave-A");

        // B "apunta" a la referencia de A: la bóveda no se la abre.
        Certificado robado = Certificado.registrar(negocioB, "robado", "x", "1", LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1), deA.getReferenciaKms());
        when(certificados.findByNegocioIdAndEstado(negocioB, EstadoCertificado.ACTIVO)).thenReturn(List.of(robado));
        Factura deB = factura(negocioB);

        assertThatThrownBy(() -> firma.firmar(deB.getId())).isInstanceOf(RuntimeException.class);
        verify(deB, never()).firmar(anyString(), anyString());
        assertThat(new String(boveda.leer(negocioA, deA.getReferenciaKms()), StandardCharsets.ISO_8859_1))
                .contains("clave-A");
    }

    @Test
    void sinCertificadoNoSeFirma() {
        Factura deB = factura(negocioB);

        assertThatThrownBy(() -> firma.firmar(deB.getId())).isInstanceOf(ReglaDeNegocioException.class);
    }

    private Certificado registrar(UUID negocio, String alias, byte[] p12, String clave) {
        en(negocio);
        ArgumentCaptor<Certificado> captor = ArgumentCaptor.forClass(Certificado.class);
        when(certificados.save(captor.capture())).thenAnswer(i -> i.getArgument(0));
        firma.registrarCertificado(new SolicitudDeCertificado(alias, Base64.getEncoder().encodeToString(p12), clave));
        Certificado c = captor.getValue();
        guardados.add(c);
        when(certificados.findByNegocioIdAndEstado(negocio, EstadoCertificado.ACTIVO)).thenReturn(
                guardados.stream().filter(g -> g.getNegocioId().equals(negocio)).toList());
        return c;
    }

    private Factura factura(UUID negocio) {
        en(negocio);
        Factura f = mock(Factura.class);
        UUID id = UUID.randomUUID();
        when(f.getId()).thenReturn(id);
        when(f.getNegocioId()).thenReturn(negocio);
        when(f.getNumeroCompleto()).thenReturn("SETT-1");
        when(f.getTotal()).thenReturn(new BigDecimal("50000"));
        when(f.getFechaEmision()).thenReturn(OffsetDateTime.now());
        when(f.getMoneda()).thenReturn("COP");
        when(facturas.findByIdAndNegocioId(id, negocio)).thenReturn(Optional.of(f));
        return f;
    }

    private static void en(UUID negocio) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, UUID.randomUUID(), "", "", Set.of("ADMINISTRADOR"),
                Set.of("FACTURACION"), Set.of("FACTURACION_RESOLUCION_EDITAR"), Set.of()));
    }

    private static String llave() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }
}
