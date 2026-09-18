package com.regenta.reservas.infra;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.reservas.aplicacion.ClienteDeLaReserva;
import com.regenta.reservas.aplicacion.ConsultaDeClientes;

/**
 * Le pregunta a servicio-clientes por el huésped al cerrar la estancia
 * (HU-118). Va con las cabeceras de quien hace el check-out: servicio-clientes
 * filtra por negocio con RLS, así que un cliente de otro negocio ni siquiera
 * aparece.
 *
 * <p>Best-effort a propósito: una estancia con saldo liquidado no se queda sin
 * cerrar porque el CRM esté caído o el cliente ya no exista. Sin snapshot la
 * factura sale al adquiriente genérico, y eso se corrige después.
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
    public Optional<ClienteDeLaReserva> consultar(UUID clienteId) {
        try {
            Map<String, Object> cliente = http.get()
                    .uri("/api/clientes/{clienteId}", clienteId)
                    .headers(this::conLaIdentidadDeQuienFactura)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (peticion, respuesta) -> {
                        throw new IllegalStateException("Ese cliente no existe");
                    })
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            if (cliente == null) {
                return Optional.empty();
            }
            return Optional.of(new ClienteDeLaReserva(clienteId, texto(cliente.get("nombreDisplay")),
                    texto(cliente.get("tipoDocumento")), texto(cliente.get("numeroDocumento")),
                    texto(cliente.get("digitoVerificacion"))));
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar el cliente {} para el snapshot de la estancia: {}",
                    clienteId, fallo.getMessage());
            return Optional.empty();
        }
    }

    private void conLaIdentidadDeQuienFactura(HttpHeaders cabeceras) {
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
