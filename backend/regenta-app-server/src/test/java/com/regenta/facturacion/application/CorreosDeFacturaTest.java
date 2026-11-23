package com.regenta.facturacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.facturacion.domain.EventoDeTransmision;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.infrastructure.FacturaRepositorio;
import com.regenta.facturacion.infrastructure.TransmisionesLog;
import com.regenta.shared.correo.CorreoEnMemoria;
import com.regenta.shared.correo.CorreoSaliente.Correo;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-122 · La factura al cliente por el correo común: con el XML adjunto, sin bloquear y con reintento. */
class CorreosDeFacturaTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID facturaId = UUID.randomUUID();
    private final byte[] xml = "<Invoice/>".getBytes(StandardCharsets.UTF_8);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CorreoEnMemoria correo = new CorreoEnMemoria();
    private final AlmacenDeDocumentos documentos = mock(AlmacenDeDocumentos.class);
    private final FacturaRepositorio facturas = mock(FacturaRepositorio.class);
    private final TransmisionesLog transmisiones = mock(TransmisionesLog.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final CorreosDeFactura correos = new CorreosDeFactura(jdbc, correo, documentos, facturas, transmisiones,
            eventos);
    private final Factura factura = mock(Factura.class);

    @BeforeEach
    void preparar() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"), Set.of("FACTURACION"),
                Set.of("FACTURACION_FACTURA_VER"), Set.of()));
        when(factura.getId()).thenReturn(facturaId);
        when(factura.getNegocioId()).thenReturn(negocio);
        when(factura.getNumeroCompleto()).thenReturn("SETT-1");
        when(factura.getTotal()).thenReturn(new BigDecimal("248000.0000"));
        when(factura.getCufe()).thenReturn("cufe-123");
        when(factura.getXmlUrl()).thenReturn("memoria://regenta/" + negocio + "/facturas/x/sett-1.xml");
        when(documentos.leer(eq(negocio), anyString())).thenReturn(xml);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("C1 / C3 · Sale con el XML adjunto, y queda ENVIADO con el identificador del proveedor")
    void sale() {
        assertThat(correos.encolarYEnviar(factura, "cliente@correo.co")).isTrue();

        Correo enviado = correo.enviados().get(0);
        assertThat(enviado.negocioId()).isEqualTo(negocio);
        assertThat(enviado.para()).isEqualTo("cliente@correo.co");
        assertThat(enviado.asunto()).isEqualTo("Factura electrónica SETT-1");
        assertThat(enviado.cuerpo()).contains("$ 248000").contains("CUFE: cufe-123");
        assertThat(enviado.adjuntos()).singleElement().satisfies(a -> {
            assertThat(a.nombre()).isEqualTo("SETT-1.xml");
            assertThat(a.contenido()).isEqualTo(xml);
        });
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("SET estado = 'ENVIADO'"), args.capture());
        assertThat((String) args.getValue()[1]).startsWith("<memoria-");
        verify(transmisiones).registrar(eq(negocio), eq(facturaId), eq(EventoDeTransmision.EMAIL_CLIENTE),
                eq("cliente@correo.co"), any(), anyString(), any(), any(), eq("Factura enviada al cliente"), any());
        verify(eventos).registrar(eq(negocio), eq("factura"), eq(facturaId), eq("factura_enviada_al_cliente"), anyMap());
    }

    @Test
    @DisplayName("C4 · Con el proveedor caído no se lanza nada: queda PENDIENTE con el motivo y se reintenta en 1 minuto")
    void proveedorCaido() {
        correo.fallarCon("Connection refused");

        assertThat(correos.encolarYEnviar(factura, "cliente@correo.co")).isFalse();

        verify(jdbc).update(contains("SET estado = ?, intentos = ?, error = ?"), eq("PENDIENTE"), eq(1),
                eq("Connection refused"), eq(false), eq("1"), any(UUID.class));
        verify(eventos, never()).registrar(any(), any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("C4 · Si el XML no se puede leer (almacén caído) también queda para reintentar")
    void sinXml() {
        when(documentos.leer(eq(negocio), anyString())).thenThrow(new RuntimeException("El almacén no responde"));

        assertThat(correos.encolarYEnviar(factura, "cliente@correo.co")).isFalse();

        verify(jdbc).update(contains("SET estado = ?, intentos = ?, error = ?"), eq("PENDIENTE"), eq(1),
                eq("El almacén no responde"), eq(false), eq("1"), any(UUID.class));
    }

    @Test
    @DisplayName("C2 · El barrido reintenta; al quinto fallo queda FALLIDO con el motivo, sin más reintentos")
    void seRinde() {
        UUID id = UUID.randomUUID();
        when(jdbc.queryForList(contains("estado = 'PENDIENTE'"), eq(negocio))).thenReturn(List.of(
                Map.of("id", id, "factura_id", facturaId, "destinatario", "cliente@correo.co", "intentos", 4)));
        when(facturas.findByIdAndNegocioId(facturaId, negocio)).thenReturn(Optional.of(factura));
        correo.fallarCon("550 5.1.1 Mailbox does not exist");

        assertThat(correos.reintentarPendientes()).isZero();

        verify(jdbc).update(contains("SET estado = ?, intentos = ?, error = ?"), eq("FALLIDO"), eq(5),
                eq("550 5.1.1 Mailbox does not exist"), eq(true), eq("25"), eq(id));
    }

    @Test
    @DisplayName("El barrido envía lo que quedó pendiente cuando el proveedor vuelve")
    void reintentoExitoso() {
        UUID id = UUID.randomUUID();
        when(jdbc.queryForList(contains("estado = 'PENDIENTE'"), eq(negocio))).thenReturn(List.of(
                Map.of("id", id, "factura_id", facturaId, "destinatario", "cliente@correo.co", "intentos", 2)));
        when(facturas.findByIdAndNegocioId(facturaId, negocio)).thenReturn(Optional.of(factura));

        assertThat(correos.reintentarPendientes()).isEqualTo(1);

        verify(jdbc).update(contains("SET estado = 'ENVIADO'"), eq(3), anyString(), eq(id));
    }
}
