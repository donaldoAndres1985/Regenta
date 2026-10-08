package com.regenta.ventas.infra;

import java.time.Duration;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.regenta.comun.barridos.Barrido;
import com.regenta.ventas.aplicacion.SagaDeConfirmacionDeVenta;

/**
 * Los barridos automáticos de servicio-ventas (HU-124). Hacen lo mismo que
 * los endpoints manuales, pero en todos los negocios y sin que nadie llame.
 */
@Configuration
public class BarridosDeVentas {

    /**
     * HU-038 criterio 4: la saga que esperó a Inventario más de su timeout se
     * compensa. {@code tomarVencidas} bloquea con {@code SKIP LOCKED}, así que
     * dos instancias no compensan la misma.
     */
    @Bean
    public Barrido barridoDeSagasVencidas(SagaDeConfirmacionDeVenta saga) {
        return new Barrido("sagas-vencidas", """
                SELECT DISTINCT negocio_id FROM ventas.sagas
                 WHERE estado = 'ESPERANDO_STOCK' AND timeout_en < now()
                """, Set.of("VENTAS"), Set.of(), Duration.ofSeconds(30), saga::compensarVencidas);
    }
}
