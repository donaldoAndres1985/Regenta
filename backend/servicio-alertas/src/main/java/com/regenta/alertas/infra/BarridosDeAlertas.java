package com.regenta.alertas.infra;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.regenta.alertas.aplicacion.GestionDeDespacho;
import com.regenta.comun.barridos.Barrido;

/**
 * Los barridos automáticos de servicio-alertas (HU-124). Hacen lo mismo que
 * los endpoints manuales, pero en todos los negocios y sin que nadie llame.
 */
@Configuration
public class BarridosDeAlertas {

    /** HU-094 criterio 3: las entregas pendientes o fallidas cuya hora de reintento llegó. */
    @Bean
    public Barrido barridoDeEntregasPorReintentar(GestionDeDespacho despacho) {
        return new Barrido("entregas-por-reintentar", """
                SELECT DISTINCT negocio_id FROM alertas.entregas
                 WHERE estado IN ('PENDIENTE', 'FALLIDA') AND intentos < 5
                   AND (proximo_intento IS NULL OR proximo_intento <= now())
                """, Set.of("ALERTAS"), Set.of("ALERTAS_ALERTA_EDITAR"), Duration.ofMinutes(1),
                despacho::reintentarPendientes);
    }
}
