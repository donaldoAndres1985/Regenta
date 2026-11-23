package com.regenta.facturacion.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import com.regenta.facturacion.application.ParametrosDeLaDian;

/**
 * HU-125 C5 · El servicio web de la DIAN en {@code /actuator/health} (componente {@code dian}). Sin
 * {@code DIAN_URL} es la simulada, que se muestra como {@code UNKNOWN} y no tumba el estado general.
 * Configurada, se pide la página del servicio como mucho una vez por minuto. Si la DIAN está caída, se
 * ve aquí; las facturas igual se reintentan y, si hace falta, entran en contingencia (HU-057).
 */
@Component("dian")
public class SaludDeLaDian implements HealthIndicator {

    private static final Duration VIGENCIA = Duration.ofMinutes(1);
    private static final Duration ESPERA = Duration.ofSeconds(5);

    private final ParametrosDeLaDian parametros;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(ESPERA).build();
    private volatile Health ultima;
    private volatile Instant cuando = Instant.EPOCH;

    public SaludDeLaDian(ParametrosDeLaDian parametros) {
        this.parametros = parametros;
    }

    @Override
    public Health health() {
        if (parametros.url().isEmpty()) {
            return Health.status(Status.UNKNOWN).withDetail("modo", "simulada").build();
        }
        Instant ahora = Instant.now();
        if (ultima == null || cuando.plus(VIGENCIA).isBefore(ahora)) {
            ultima = consultar();
            cuando = ahora;
        }
        return ultima;
    }

    private Health consultar() {
        try {
            HttpResponse<Void> r = http.send(HttpRequest.newBuilder(URI.create(parametros.url())).timeout(ESPERA)
                    .GET().build(), HttpResponse.BodyHandlers.discarding());
            return r.statusCode() < 500 ? Health.up().withDetail("ambiente", parametros.ambiente()).build()
                    : Health.down().withDetail("http", r.statusCode()).build();
        } catch (InterruptedException cortado) {
            Thread.currentThread().interrupt();
            return Health.down().withDetail("error", "interrumpido").build();
        } catch (Exception caida) {
            return Health.down().withDetail("error", caida.getClass().getSimpleName()).build();
        }
    }
}
