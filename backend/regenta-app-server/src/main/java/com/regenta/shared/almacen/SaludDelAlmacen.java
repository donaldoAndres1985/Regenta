package com.regenta.shared.almacen;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * HU-125 C5 · El almacén de documentos en {@code /actuator/health} (componente {@code almacen}). Pregunta al
 * S3 como mucho una vez por minuto: el healthcheck lo consultan seguido y no debe convertirse en tráfico
 * al proveedor.
 */
@Component("almacen")
public class SaludDelAlmacen implements HealthIndicator {

    private static final Duration VIGENCIA = Duration.ofMinutes(1);

    private final AlmacenDeObjetos almacen;
    private final Clock reloj;
    private volatile Health ultima;
    private volatile Instant cuando = Instant.EPOCH;

    @Autowired
    public SaludDelAlmacen(AlmacenDeObjetos almacen) {
        this(almacen, Clock.systemUTC());
    }

    SaludDelAlmacen(AlmacenDeObjetos almacen, Clock reloj) {
        this.almacen = almacen;
        this.reloj = reloj;
    }

    @Override
    public Health health() {
        Instant ahora = reloj.instant();
        if (ultima == null || cuando.plus(VIGENCIA).isBefore(ahora)) {
            ultima = consultar();
            cuando = ahora;
        }
        return ultima;
    }

    private Health consultar() {
        try {
            almacen.comprobar();
            return Health.up().withDetail("tipo", almacen.getClass().getSimpleName()).build();
        } catch (RuntimeException caido) {
            return Health.down().withDetail("error", caido.getClass().getSimpleName()).build();
        }
    }
}
