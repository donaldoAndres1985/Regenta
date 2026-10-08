package com.regenta.clientes.infra;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.regenta.clientes.aplicacion.GestionDeInteracciones;
import com.regenta.comun.barridos.Barrido;

/**
 * Los barridos automáticos de servicio-clientes (HU-124). Hacen lo mismo que
 * los endpoints manuales, pero en todos los negocios y sin que nadie llame.
 */
@Configuration
public class BarridosDeClientes {

    /**
     * HU-024 criterio 3: un recordatorio por cada seguimiento cuya fecha llegó.
     * {@code tomarSeguimientosVencidos} bloquea con {@code SKIP LOCKED}.
     */
    @Bean
    public Barrido barridoDeSeguimientosVencidos(GestionDeInteracciones interacciones) {
        return new Barrido("seguimientos-vencidos", """
                SELECT DISTINCT negocio_id FROM crm.interacciones
                 WHERE seguimiento_en IS NOT NULL AND seguimiento_en <= current_date
                   AND seguimiento_notificado_en IS NULL
                """, Set.of("CLIENTES"), Set.of(), Duration.ofMinutes(15),
                () -> interacciones.procesarSeguimientosPendientes(null));
    }
}
