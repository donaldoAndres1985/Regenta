package com.regenta.reportes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoExpiradoException;
import com.regenta.shared.web.ServicioNoDisponibleException;

/** HU-121 · Los archivos exportados en el almacén: guardar, descargar, subir los viejos y vencer. */
class ArchivosDeReporteTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID ejecucion = UUID.randomUUID();
    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ArchivosDeReporte archivos = new ArchivosDeReporte(jdbc, almacen, Duration.ofDays(30));
    private final byte[] xlsx = {80, 75, 3, 4};

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, UUID.randomUUID(), "", "", Set.of(),
                Set.of("REPORTES"), Set.of("REPORTES_REPORTE_VER"), Set.of()));
        when(jdbc.query(contains("SELECT url FROM reportes.archivos_reporte"), any(RowMapper.class), eq(ejecucion)))
                .thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void fila(String url, byte[] contenido, OffsetDateTime expiradoEn) {
        Map<String, Object> f = new HashMap<>();
        f.put("nombre", "ventas.xlsx");
        f.put("tipo_mime", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        f.put("contenido", contenido);
        f.put("url", url);
        f.put("expirado_en", expiradoEn);
        when(jdbc.queryForList(contains("FROM reportes.archivos_reporte"), eq(ejecucion), eq(negocio)))
                .thenReturn(List.of(f));
    }

    @Test
    @DisplayName("C1 · El archivo va al almacén; en la base, su URL (sin contenido) y cuándo vence")
    void guardar() {
        archivos.guardar(negocio, ejecucion, "ventas.xlsx", "application/xlsx", xlsx);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("INSERT INTO reportes.archivos_reporte"), args.capture());
        Object[] a = args.getValue();
        assertThat((String) a[5]).startsWith("memoria://regenta/" + negocio + "/reportes/");
        Instant vence = ((Timestamp) a[6]).toInstant();
        assertThat(vence).isBetween(Instant.now().plus(Duration.ofDays(29)), Instant.now().plus(Duration.ofDays(31)));
        assertThat(almacen.leer(negocio, (String) a[5])).isEqualTo(xlsx);
    }

    @Test
    @DisplayName("C2 · Se descarga del almacén con la sesión del negocio dueño")
    void descargar() {
        String url = almacen.guardar(negocio, "reportes", "ventas.xlsx", xlsx, "x");
        fila(url, null, null);

        assertThat(archivos.leer(ejecucion).contenido()).isEqualTo(xlsx);
    }

    @Test
    @DisplayName("C2 · Una URL de otro negocio no se descarga aunque alguien la meta en la fila")
    void urlAjena() {
        String ajena = almacen.guardar(UUID.randomUUID(), "reportes", "ventas.xlsx", xlsx, "x");
        fila(ajena, null, null);

        assertThatThrownBy(() -> archivos.leer(ejecucion)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("C4 · Uno que todavía está en la base se descarga igual mientras el barrido lo sube")
    void legado() {
        fila(null, xlsx, null);

        assertThat(archivos.leer(ejecucion).contenido()).isEqualTo(xlsx);
    }

    @Test
    @DisplayName("C5 · Uno expirado responde 410: se generó bien, su archivo ya no está")
    void expirado() {
        fila(null, null, OffsetDateTime.now());

        assertThatThrownBy(() -> archivos.leer(ejecucion)).isInstanceOf(RecursoExpiradoException.class);
    }

    @Test
    @DisplayName("Con el almacén caído la descarga responde 503, no 404")
    void almacenCaido() {
        String url = almacen.guardar(negocio, "reportes", "ventas.xlsx", xlsx, "x");
        fila(url, null, null);
        almacen.simularCaida(true);

        assertThatThrownBy(() -> archivos.leer(ejecucion)).isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    @DisplayName("C4 · El barrido sube los que están en la base: deja la URL, vacía la columna y fija el vencimiento")
    void subirLosViejos() {
        Instant creado = Instant.parse("2026-11-01T10:00:00Z");
        Map<String, Object> viejo = new HashMap<>(Map.of("ejecucion_id", ejecucion, "nombre", "ventas.xlsx",
                "tipo_mime", "x", "contenido", xlsx, "creado_en", Timestamp.from(creado)));
        when(jdbc.queryForList(contains("contenido IS NOT NULL"), eq(negocio))).thenReturn(List.of(viejo));

        assertThat(archivos.subirLosDeLaBase()).isEqualTo(1);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(contains("SET url = ?, contenido = NULL"), args.capture());
        assertThat(almacen.leer(negocio, (String) args.getValue()[0])).isEqualTo(xlsx);
        assertThat(((Timestamp) args.getValue()[1]).toInstant()).isEqualTo(creado.plus(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("C5 · El barrido borra del almacén los vencidos y los marca expirados")
    void vencer() {
        String url = almacen.guardar(negocio, "reportes", "ventas.xlsx", xlsx, "x");
        when(jdbc.queryForList(contains("expira_en <= now()"), eq(negocio)))
                .thenReturn(List.of(new HashMap<>(Map.of("ejecucion_id", ejecucion, "url", url))));

        assertThat(archivos.borrarVencidos()).isEqualTo(1);

        assertThat(almacen.cuantos()).isZero();
        verify(jdbc).update(contains("SET url = NULL, expirado_en = now()"), eq(ejecucion));
    }

    @Test
    @DisplayName("Volver a generar la misma ejecución borra el archivo anterior del almacén")
    void regenerar() {
        String anterior = almacen.guardar(negocio, "reportes", "ventas.xlsx", xlsx, "x");
        when(jdbc.query(contains("SELECT url FROM reportes.archivos_reporte"), any(RowMapper.class), eq(ejecucion)))
                .thenReturn(List.of(anterior));

        archivos.guardar(negocio, ejecucion, "ventas.xlsx", "x", xlsx);

        assertThat(almacen.cuantos()).isEqualTo(1);
        assertThatThrownBy(() -> almacen.leer(negocio, anterior)).isInstanceOf(NoEncontradoException.class);
        verify(jdbc).update(anyString(), any(Object[].class));
    }
}
