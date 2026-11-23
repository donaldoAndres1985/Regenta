package com.regenta.auditoria.application;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.regenta.usuarios.api.PlanDelNegocioApi;

/**
 * HU-131 · Cuántos días conserva un negocio su bitácora en la tabla viva. Si el negocio la configuró,
 * esa. Si no (C4), la de su plan, y queda escrita en {@code auditoria.politicas_retencion} con
 * {@code origen = 'PLAN'} y el plan del que salió, para que se sepa cuál se aplicó.
 */
@Component
public class RetencionDeBitacora {

    /** Si el plan no se conoce o no tiene retención configurada: un año. */
    static final int POR_DEFECTO = 365;

    private final JdbcTemplate jdbc;
    private final PlanDelNegocioApi planes;
    private final Map<String, Integer> diasPorPlan;

    @org.springframework.beans.factory.annotation.Autowired
    public RetencionDeBitacora(JdbcTemplate jdbc, PlanDelNegocioApi planes,
            @Value("${regenta.auditoria.retencion.basico:365}") int basico,
            @Value("${regenta.auditoria.retencion.profesional:730}") int profesional,
            @Value("${regenta.auditoria.retencion.empresarial:1825}") int empresarial) {
        this(jdbc, planes, Map.of("BASICO", basico, "PROFESIONAL", profesional, "EMPRESARIAL", empresarial));
    }

    RetencionDeBitacora(JdbcTemplate jdbc, PlanDelNegocioApi planes, Map<String, Integer> diasPorPlan) {
        this.jdbc = jdbc;
        this.planes = planes;
        this.diasPorPlan = diasPorPlan;
    }

    /** La retención con que se archiva y de dónde salió. */
    public record Retencion(int dias, String origen, String plan) {
    }

    /** La del negocio; si no tiene, la de su plan, y la deja escrita (C4). */
    public Retencion de(UUID negocioId) {
        List<Retencion> guardada = jdbc.query(
                "SELECT dias_auditoria, origen, plan FROM auditoria.politicas_retencion WHERE negocio_id = ?",
                (rs, n) -> new Retencion(rs.getInt(1), rs.getString(2), rs.getString(3)), negocioId);
        if (!guardada.isEmpty()) {
            return guardada.get(0);
        }
        String plan = planes.planDe(negocioId).map(p -> p.toUpperCase(Locale.ROOT)).orElse(null);
        int dias = plan == null ? POR_DEFECTO : diasPorPlan.getOrDefault(plan, POR_DEFECTO);
        jdbc.update("""
                INSERT INTO auditoria.politicas_retencion (negocio_id, dias_auditoria, origen, plan)
                VALUES (?, ?, 'PLAN', ?)
                ON CONFLICT (negocio_id) DO NOTHING
                """, negocioId, dias, plan);
        return new Retencion(dias, "PLAN", plan);
    }
}
