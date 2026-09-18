package com.regenta.ventas.infra;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.ventas.aplicacion.ClienteDeLaVenta;
import com.regenta.ventas.aplicacion.ConsultaDeClientes;

/**
 * Le pregunta a servicio-clientes por el cliente que se quiere asignar
 * (HU-113, contra lo que expuso HU-021).
 *
 * <p>Va con las cabeceras de quien está vendiendo, igual que la consulta de
 * cupo de HU-040: servicio-clientes filtra por negocio con RLS, así que un
 * cliente de otro negocio responde 404 allá y 404 acá (criterio 3).
 */
@Component
public class ConsultaDeClientesHttp implements ConsultaDeClientes {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeClientesHttp.class);

    private final RestClient http;

    public ConsultaDeClientesHttp(RestClient.Builder builder,
            @Value("${regenta.clientes.url:http://localhost:8082}") String urlDeClientes) {
        this.http = builder.baseUrl(urlDeClientes).build();
    }

    @Override
    public ClienteDeLaVenta consultar(UUID clienteId) {
        Map<String, Object> cliente;
        try {
            cliente = http.get()
                    .uri("/api/clientes/{clienteId}", clienteId)
                    .headers(this::conLaIdentidadDeQuienVende)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (peticion, respuesta) -> {
                        throw new NoEncontradoException("Ese cliente no existe");
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (NoEncontradoException noExiste) {
            throw noExiste;
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar el cliente {}: {}", clienteId, fallo.getMessage());
            // Nunca se bloquea el cobro por no poder buscar un cliente: se
            // vende a consumidor final y se corrige después
            // (design/comportamiento/ClienteVenta.md, "Qué NO debe pasar").
            throw new ReglaDeNegocioException(
                    "No se pudo consultar el cliente: sigue la venta a consumidor final");
        }
        if (cliente == null) {
            throw new NoEncontradoException("Ese cliente no existe");
        }
        return new ClienteDeLaVenta(clienteId, texto(cliente.get("nombreDisplay")),
                texto(cliente.get("tipoDocumento")), texto(cliente.get("numeroDocumento")),
                texto(cliente.get("digitoVerificacion")));
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

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
