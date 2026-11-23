package com.regenta.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ServicioNoDisponibleException;

/**
 * HU-131 C3 · Un evento archivado se recupera al buscarlo con {@code incluirArchivados}: se lee su
 * archivo del almacén (JSON por línea, comprimido, como lo deja el archivador), viene marcado y no se
 * repite si también estaba vivo.
 */
class BitacoraArchivadaTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID venta = UUID.randomUUID();
    private final UUID otraVenta = UUID.randomUUID();
    private final UUID archivadoId = UUID.randomUUID();
    private final UUID repetidoId = UUID.randomUUID();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final ConsultaDeAuditoria consulta = new ConsultaDeAuditoria(jdbc, new ObjectMapper(), almacen);
    private String url;

    private String evento(UUID id, UUID negocioId, UUID entidad, String cuando) {
        return "{\"id\":\"" + id + "\",\"negocio_id\":\"" + negocioId + "\",\"usuario_id\":\"" + usuario
                + "\",\"servicio\":\"servicio-ventas\",\"modulo\":null,\"entidad_tipo\":\"Venta\",\"entidad_id\":\""
                + entidad + "\",\"accion\":\"ANULAR\",\"cambios\":{\"estado\":{\"antes\":\"CONFIRMADA\","
                + "\"despues\":\"ANULADA\"}},\"trace_id\":\"t-1\",\"resultado\":\"OK\",\"ocurrido_en\":\"" + cuando + "\"}";
    }

    @BeforeEach
    void preparar() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, usuario, "", "", Set.of(), Set.of("AUDITORIA"),
                Set.of("AUDITORIA_BITACORA_VER"), Set.of()));
        byte[] gz = ArchivadorDeBitacora.comprimir(List.of(
                new String[] {archivadoId.toString(), evento(archivadoId, negocio, venta, "2025-01-15T10:00:00+00:00")},
                new String[] {repetidoId.toString(), evento(repetidoId, negocio, venta, "2025-01-16T10:00:00+00:00")},
                new String[] {"x", evento(UUID.randomUUID(), negocio, otraVenta, "2025-01-17T10:00:00+00:00")},
                // Una línea de otro negocio en el archivo no se devuelve nunca.
                new String[] {"y", evento(UUID.randomUUID(), UUID.randomUUID(), venta, "2025-01-18T10:00:00+00:00")}));
        url = almacen.guardar(negocio, "bitacora", "bitacora-2025-01.jsonl.gz", gz, "application/gzip");
        when(jdbc.queryForList(contains("FROM auditoria.archivos_auditoria WHERE negocio_id = ? ORDER BY desde"),
                eq(String.class), eq(negocio))).thenReturn(List.of(url));
        when(jdbc.queryForList(contains("desde < ? AND hasta >= ?"), eq(String.class), eq(negocio), any(), any()))
                .thenReturn(List.of(url));
        // Vivo: uno reciente, y el «repetido» por si quedó en los dos lados.
        EventoDeAuditoria vivo = new EventoDeAuditoria(UUID.randomUUID(), usuario, "servicio-ventas", "Venta", venta,
                "ACTUALIZAR", "t-2", "OK", OffsetDateTime.parse("2026-11-01T10:00:00Z"), null, false);
        EventoDeAuditoria repetido = new EventoDeAuditoria(repetidoId, usuario, "servicio-ventas", "Venta", venta,
                "ANULAR", "t-1", "OK", OffsetDateTime.parse("2025-01-16T10:00:00Z"), null, false);
        when(jdbc.query(contains("entidad_tipo = ? AND entidad_id = ?"), any(RowMapper.class), eq(negocio),
                eq("Venta"), eq(venta))).thenReturn(List.of(vivo, repetido));
        when(jdbc.query(contains("usuario_id = ? AND ocurrido_en"), any(RowMapper.class), eq(negocio), eq(usuario),
                any(), any())).thenReturn(List.of());
        when(jdbc.query(contains("trace_id = ?"), any(RowMapper.class), eq(negocio), eq("t-1")))
                .thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("Sin pedirlo, la búsqueda sigue siendo solo la tabla viva (rápida)")
    void soloVivos() {
        assertThat(consulta.porEntidad("Venta", venta)).hasSize(2).noneMatch(EventoDeAuditoria::archivado);
    }

    @Test
    @DisplayName("C3 · El historial de un documento trae también lo archivado, marcado y sin repetir")
    void porEntidad() {
        List<EventoDeAuditoria> eventos = consulta.porEntidad("Venta", venta, true);

        assertThat(eventos).extracting(EventoDeAuditoria::id).doesNotHaveDuplicates().contains(archivadoId, repetidoId);
        assertThat(eventos).hasSize(3);
        EventoDeAuditoria archivado = eventos.stream().filter(e -> e.id().equals(archivadoId)).findFirst().orElseThrow();
        assertThat(archivado.archivado()).isTrue();
        assertThat(archivado.accion()).isEqualTo("ANULAR");
        assertThat(archivado.usuarioId()).isEqualTo(usuario);
        assertThat(archivado.cambios()).containsKey("estado");
        // Lo más reciente primero, como en la tabla viva.
        assertThat(eventos.get(0).ocurridoEn()).isEqualTo(OffsetDateTime.parse("2026-11-01T10:00:00Z"));
    }

    @Test
    @DisplayName("C3 · Por usuario y rango: solo lo del rango; por trace, todo lo del request")
    void porUsuarioYTrace() {
        assertThat(consulta.porUsuario(usuario, OffsetDateTime.parse("2025-01-15T00:00:00Z"),
                OffsetDateTime.parse("2025-01-16T00:00:00Z"), true)).extracting(EventoDeAuditoria::id)
                .containsExactly(archivadoId);
        assertThat(consulta.porTraceId("t-1", true)).hasSize(3).allMatch(EventoDeAuditoria::archivado);
    }

    @Test
    @DisplayName("Con el almacén caído, buscar en lo archivado responde 503, no una lista vacía")
    void almacenCaido() {
        almacen.simularCaida(true);

        assertThatThrownBy(() -> consulta.porEntidad("Venta", venta, true))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("El archivo es un JSON por línea, comprimido")
    void formato() throws IOException {
        byte[] gz = ArchivadorDeBitacora.comprimir(List.<String[]>of(new String[] {"a", "{\"id\":1}"},
                new String[] {"b", "{\"id\":2}"}));
        String texto = new String(new GZIPInputStream(new ByteArrayInputStream(gz)).readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(texto).isEqualTo("{\"id\":1}\n{\"id\":2}\n");
    }
}
