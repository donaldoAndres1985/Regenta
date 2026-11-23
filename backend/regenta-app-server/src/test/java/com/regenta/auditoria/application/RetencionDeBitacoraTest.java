package com.regenta.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.regenta.usuarios.api.PlanDelNegocioApi;

/** HU-131 C4 · La retención del negocio; si no la configuró, la de su plan, y queda escrita. */
class RetencionDeBitacoraTest {

    private final UUID negocio = UUID.randomUUID();
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PlanDelNegocioApi planes = mock(PlanDelNegocioApi.class);
    private final RetencionDeBitacora retencion = new RetencionDeBitacora(jdbc, planes,
            Map.of("BASICO", 365, "PROFESIONAL", 730, "EMPRESARIAL", 1825));

    @Test
    @DisplayName("La que configuró el negocio manda, y no se consulta el plan")
    void configurada() {
        when(jdbc.query(contains("FROM auditoria.politicas_retencion"), any(RowMapper.class), eq(negocio)))
                .thenReturn(List.of(new RetencionDeBitacora.Retencion(90, "NEGOCIO", null)));

        assertThat(retencion.de(negocio)).isEqualTo(new RetencionDeBitacora.Retencion(90, "NEGOCIO", null));
        verify(planes, never()).planDe(any());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("C4 · Sin configurar, se aplica la del plan y queda escrita con su origen y el plan")
    void delPlan() {
        when(jdbc.query(contains("FROM auditoria.politicas_retencion"), any(RowMapper.class), eq(negocio)))
                .thenReturn(List.of());
        when(planes.planDe(negocio)).thenReturn(Optional.of("Empresarial"));

        assertThat(retencion.de(negocio)).isEqualTo(new RetencionDeBitacora.Retencion(1825, "PLAN", "EMPRESARIAL"));
        verify(jdbc).update(contains("INSERT INTO auditoria.politicas_retencion"), eq(negocio), eq(1825),
                eq("EMPRESARIAL"));
    }

    @Test
    @DisplayName("C4 · Un plan desconocido usa un año, y también queda escrito")
    void planDesconocido() {
        when(jdbc.query(contains("FROM auditoria.politicas_retencion"), any(RowMapper.class), eq(negocio)))
                .thenReturn(List.of());
        when(planes.planDe(negocio)).thenReturn(Optional.empty());

        assertThat(retencion.de(negocio).dias()).isEqualTo(365);
        verify(jdbc).update(contains("INSERT INTO auditoria.politicas_retencion"), eq(negocio), eq(365), eq(null));
    }
}
