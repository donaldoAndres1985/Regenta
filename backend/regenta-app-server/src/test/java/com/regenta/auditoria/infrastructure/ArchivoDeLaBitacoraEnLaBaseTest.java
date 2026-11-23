package com.regenta.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.PostgresDePrueba;
import com.regenta.auditoria.application.ArchivadorDeBitacora;
import com.regenta.auditoria.application.ConsultaDeAuditoria;
import com.regenta.auditoria.application.EventoDeAuditoria;
import com.regenta.auditoria.application.RetencionDeBitacora;
import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.usuarios.api.PlanDelNegocioApi;

/**
 * HU-131 contra PostgreSQL (auditoria V4), con dos negocios de retención distinta en la misma partición:
 * el A guarda 30 días y el B un año. Archivar al A no toca al B (C2), lo archivado se recupera (C3), la
 * tabla sigue siendo append-only para lo no archivado, y sin retención propia se aplica y anota la del
 * plan (C4).
 */
@DataJpaTest
class ArchivoDeLaBitacoraEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID venta = UUID.randomUUID();
    private final OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final PlanDelNegocioApi planes = mock(PlanDelNegocioApi.class);

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void como(UUID negocio) {
        comoElNegocio(jdbc, negocio);
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"), Set.of("AUDITORIA"),
                Set.of("AUDITORIA_BITACORA_VER"), Set.of()));
    }

    private void retencion(UUID negocio, int dias) {
        como(negocio);
        jdbc.update("INSERT INTO auditoria.politicas_retencion (negocio_id, dias_auditoria) VALUES (?, ?)", negocio, dias);
    }

    private UUID evento(UUID negocio, OffsetDateTime cuando) {
        como(negocio);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO auditoria.eventos_auditoria (id, negocio_id, servicio, entidad_tipo, entidad_id, accion,
                    cambios, trace_id, ocurrido_en)
                VALUES (?, ?, 'servicio-ventas', 'Venta', ?, 'ACTUALIZAR', '{"estado":{"antes":"A","despues":"B"}}'::jsonb,
                    't-1', ?)
                """, id, negocio, venta, cuando);
        return id;
    }

    private ArchivadorDeBitacora archivador() {
        return new ArchivadorDeBitacora(jdbc, almacen, new RetencionDeBitacora(jdbc, planes, 365, 730, 1825));
    }

    private int vivos(UUID negocio) {
        como(negocio);
        return jdbc.queryForObject("SELECT count(*) FROM auditoria.eventos_auditoria WHERE negocio_id = ?",
                Integer.class, negocio);
    }

    @Test
    @DisplayName("C1 / C2 / C3 · Se archiva lo vencido del A; el B, en la misma partición, sigue intacto; lo archivado se recupera")
    void archivarSinTocarAlOtro() {
        retencion(negocioA, 30);
        retencion(negocioB, 365);
        UUID viejoA = evento(negocioA, ahora.minusDays(90));
        UUID medioA = evento(negocioA, ahora.minusDays(40));
        UUID recienteA = evento(negocioA, ahora.minusDays(5));
        UUID viejoB = evento(negocioB, ahora.minusDays(90));

        como(negocioA);
        List<ArchivadorDeBitacora.Archivado> hechos = archivador().archivarVencidos(ahora);

        // C1: los vencidos del A salieron de la tabla viva y quedaron en archivos del almacén.
        assertThat(hechos.stream().mapToInt(ArchivadorDeBitacora.Archivado::eventos).sum()).isEqualTo(2);
        assertThat(vivos(negocioA)).isEqualTo(1);
        como(negocioA);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auditoria.archivos_auditoria WHERE negocio_id = ?",
                Integer.class, negocioA)).isEqualTo(hechos.size());
        assertThat(almacen.cuantos()).isEqualTo(hechos.size());

        // C2: el evento del B, del mismo mes, sigue vivo; y con su año de retención no se archiva.
        assertThat(vivos(negocioB)).isEqualTo(1);
        como(negocioB);
        assertThat(archivador().archivarVencidos(ahora)).isEmpty();
        assertThat(vivos(negocioB)).isEqualTo(1);

        // C3: el auditor del A lo encuentra todo, con lo archivado marcado.
        como(negocioA);
        List<EventoDeAuditoria> historial = new ConsultaDeAuditoria(jdbc, new ObjectMapper(), almacen)
                .porEntidad("Venta", venta, true);
        assertThat(historial).extracting(EventoDeAuditoria::id).containsExactlyInAnyOrder(viejoA, medioA, recienteA);
        assertThat(historial).filteredOn(EventoDeAuditoria::archivado).extracting(EventoDeAuditoria::id)
                .containsExactlyInAnyOrder(viejoA, medioA);
        assertThat(historial).filteredOn(e -> e.id().equals(viejoA)).singleElement()
                .satisfies(e -> assertThat(e.cambios()).containsKey("estado"));
        assertThat(viejoB).isNotNull();
    }

    @Test
    @DisplayName("Sigue siendo append-only: lo que ningún archivo cubre no se puede borrar")
    void noSeBorraSinArchivar() {
        UUID vivo = evento(negocioB, ahora.minusDays(90));
        como(negocioB);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM auditoria.eventos_auditoria WHERE id = ?", vivo))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }

    @Test
    @DisplayName("Un evento nunca se edita, ni siquiera archivado")
    void noSeEdita() {
        UUID vivo = evento(negocioB, ahora.minusDays(1));
        como(negocioB);

        assertThatThrownBy(() -> jdbc.update("UPDATE auditoria.eventos_auditoria SET accion = 'CREAR' WHERE id = ?",
                vivo)).isInstanceOf(DataAccessException.class).hasMessageContaining("append-only");
    }

    @Test
    @DisplayName("C4 · Sin retención propia se aplica la del plan, y queda escrita cuál y de dónde salió")
    void retencionDelPlan() {
        when(planes.planDe(negocioA)).thenReturn(Optional.of("EMPRESARIAL"));
        evento(negocioA, ahora.minusDays(400));

        como(negocioA);
        assertThat(archivador().archivarVencidos(ahora)).isEmpty(); // 400 días < 1825 del plan Empresarial

        Map<String, Object> politica = jdbc.queryForMap(
                "SELECT dias_auditoria, origen, plan FROM auditoria.politicas_retencion WHERE negocio_id = ?", negocioA);
        assertThat(politica).containsEntry("dias_auditoria", 1825).containsEntry("origen", "PLAN")
                .containsEntry("plan", "EMPRESARIAL");
        assertThat(vivos(negocioA)).isEqualTo(1);
    }
}
