package com.regenta.ventas.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.ventas.aplicacion.ConsultaDeCredito;
import com.regenta.ventas.aplicacion.CreditoDelCliente;

/**
 * Le pregunta a servicio-clientes por el cupo y la cartera del cliente
 * (HU-040, contra lo que expuso HU-022). Una sola llamada a
 * {@code GET /api/clientes/{id}/cartera} trae todo lo que hace falta: si tiene
 * crédito, cuánto le queda, a cuántos días y si está en mora.
 *
 * <p>Va con las mismas cabeceras que escribió el gateway —negocio, usuario,
 * permisos—: servicio-clientes exige {@code CLIENTES_CARTERA_VER} y filtra por
 * negocio con RLS, así que la llamada tiene que ir en nombre de quien está
 * vendiendo, no de un superusuario interno.
 *
 * <p>Si Clientes no responde, la venta a crédito <b>no pasa</b>. Aprobar
 * fiado sin poder mirar el cupo es justo lo que esta historia viene a evitar;
 * perder la venta se arregla cobrando de contado, y un cupo reventado no.
 */
@Component
public class ConsultaDeCreditoHttp implements ConsultaDeCredito {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeCreditoHttp.class);

    private final RestClient http;

    public ConsultaDeCreditoHttp(RestClient.Builder builder,
            @Value("${regenta.clientes.url:http://localhost:8082}") String urlDeClientes) {
        this.http = builder.baseUrl(urlDeClientes).build();
    }

    @Override
    public CreditoDelCliente consultar(UUID clienteId) {
        Map<String, Object> cartera;
        try {
            cartera = http.get()
                    .uri("/api/clientes/{clienteId}/cartera", clienteId)
                    .headers(this::conLaIdentidadDeQuienVende)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar la cartera del cliente {}: {}", clienteId,
                    fallo.getMessage());
            throw new ReglaDeNegocioException(
                    "No se pudo verificar el cupo del cliente: intenta de nuevo o cobra de contado");
        }
        if (cartera == null) {
            throw new ReglaDeNegocioException("No se pudo verificar el cupo del cliente");
        }
        return aCredito(cartera);
    }

    private void conLaIdentidadDeQuienVende(HttpHeaders cabeceras) {
        DatosDelNegocio quien = ContextoDeNegocio.actual();
        cabeceras.set(CabecerasDeNegocio.NEGOCIO, quien.negocio().toString());
        if (quien.usuario() != null) {
            cabeceras.set(CabecerasDeNegocio.USUARIO, quien.usuario().toString());
        }
        cabeceras.set(CabecerasDeNegocio.PLAN, quien.plan());
        cabeceras.set(CabecerasDeNegocio.PATRON, quien.patron());
        cabeceras.set(CabecerasDeNegocio.ROLES, String.join(",", quien.roles()));
        cabeceras.set(CabecerasDeNegocio.MODULOS, String.join(",", quien.modulos()));
        cabeceras.set(CabecerasDeNegocio.PERMISOS, String.join(",", quien.permisos()));
    }

    @SuppressWarnings("unchecked")
    private static CreditoDelCliente aCredito(Map<String, Object> cartera) {
        boolean habilitado = Boolean.TRUE.equals(cartera.get("creditoHabilitado"));
        if (!habilitado) {
            return CreditoDelCliente.sinCredito();
        }
        List<Map<String, Object>> cuentas =
                (List<Map<String, Object>>) cartera.getOrDefault("cuentas", List.of());
        long moraMaxima = cuentas.stream()
                .filter(c -> Boolean.TRUE.equals(c.get("vencida")))
                .mapToLong(c -> numeroEntero(c.get("diasMora")))
                .max()
                .orElse(0L);
        return CreditoDelCliente.habilitado(
                numero(cartera.get("cupo")),
                numero(cartera.get("saldo")),
                numero(cartera.get("disponible")),
                (int) numeroEntero(cartera.get("diasCredito")),
                moraMaxima > 0,
                moraMaxima);
    }

    private static BigDecimal numero(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static long numeroEntero(Object valor) {
        return valor == null ? 0L : Long.parseLong(valor.toString());
    }
}
