package com.regenta.reportes.infra;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.regenta.comun.barridos.Barrido;
import com.regenta.reportes.aplicacion.GestionDeExportaciones;
import com.regenta.reportes.aplicacion.GestionDeProgramaciones;

/**
 * Los barridos automáticos de servicio-reportes (HU-124). Hacen lo mismo que
 * los endpoints manuales, pero en todos los negocios y sin que nadie llame.
 * Los dos toman sus filas con {@code FOR UPDATE SKIP LOCKED}.
 */
@Configuration
public class BarridosDeReportes {

    /**
     * Los de quien exporta: generar el archivo también lee el reporte, y leer
     * pide {@code REPORTES_REPORTE_VER}.
     */
    private static final Set<String> PERMISOS =
            Set.of("REPORTES_REPORTE_VER", "REPORTES_REPORTE_EXPORTAR");

    /** HU-100 criterio 2: las programaciones cuyo momento llegó se encolan y se reagendan. */
    @Bean
    public Barrido barridoDeProgramacionesVencidas(GestionDeProgramaciones programaciones) {
        return new Barrido("programaciones-vencidas", """
                SELECT DISTINCT negocio_id FROM reportes.reportes_programados
                 WHERE activo AND proxima_ejecucion IS NOT NULL AND proxima_ejecucion <= now()
                """, Set.of("REPORTES"), PERMISOS, Duration.ofMinutes(1), programaciones::barrer);
    }

    /** HU-100 criterios 3 y 4: lo pedido se genera, y lo que falló se reintenta a su hora. */
    @Bean
    public Barrido barridoDeExportacionesPendientes(GestionDeExportaciones exportaciones) {
        return new Barrido("exportaciones-pendientes", """
                SELECT DISTINCT negocio_id FROM reportes.ejecuciones_reporte
                 WHERE estado IN ('EN_CURSO', 'FALLIDO')
                   AND proximo_intento_en IS NOT NULL AND proximo_intento_en <= now()
                """, Set.of("REPORTES"), PERMISOS, Duration.ofSeconds(30),
                exportaciones::procesarPendientes);
    }
}
