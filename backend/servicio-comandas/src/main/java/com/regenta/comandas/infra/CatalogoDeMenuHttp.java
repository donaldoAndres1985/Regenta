package com.regenta.comandas.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comandas.aplicacion.CatalogoDeMenu;
import com.regenta.comun.errores.ErrorDeAplicacion;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Le pregunta a servicio-menu por el ítem (HU-077) y por la selección de
 * modificadores (HU-078) al agregar una línea a la comanda (HU-085). HU-138.
 *
 * <p>Va con las cabeceras de quien toma el pedido, igual que
 * {@code ConsultaDeClientesHttp} en ventas: servicio-menu filtra por negocio,
 * así que un ítem de otro negocio responde 404 allá y no existe acá
 * (criterio 9). Si el menú no responde, la línea no se agrega: no se inventa
 * un precio (criterio 10).
 */
@Component
public class CatalogoDeMenuHttp implements CatalogoDeMenu {

    private static final Logger log = LoggerFactory.getLogger(CatalogoDeMenuHttp.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient http;

    public CatalogoDeMenuHttp(RestClient.Builder builder,
            @Value("${regenta.menu.url:http://localhost:8088}") String urlDelMenu) {
        this.http = builder.baseUrl(urlDelMenu).build();
    }

    @Override
    public ItemDeMenu item(UUID negocioId, UUID itemMenuId) {
        Map<String, Object> item;
        try {
            item = preguntar(() -> http.get()
                    .uri("/api/menu/items/{itemId}", itemMenuId)
                    .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                    .retrieve()
                    .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        } catch (NoEncontradoException noExiste) {
            return null;
        }
        if (item == null || !Boolean.TRUE.equals(item.get("activo"))) {
            return null;
        }
        return new ItemDeMenu(itemMenuId, texto(item.get("nombre")), decimal(item.get("precio")),
                uuid(item.get("estacionId")), texto(item.get("curso")),
                decimal(item.get("costoEstimado")), Boolean.TRUE.equals(item.get("disponible")));
    }

    @Override
    public Cotizacion cotizarModificadores(UUID negocioId, UUID itemMenuId,
            List<UUID> modificadorIds) {
        // Siempre se pregunta, aunque no se haya elegido nada: el menú es quien
        // sabe si el ítem tiene un grupo obligatorio.
        List<UUID> elegidos = modificadorIds == null ? List.of() : modificadorIds;
        Map<String, Object> cotizacion = preguntar(() -> http.post()
                .uri("/api/menu/items/{itemId}/seleccion-modificadores", itemMenuId)
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(elegidos)
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(new ParameterizedTypeReference<Map<String, Object>>() {}));
        if (cotizacion == null) {
            throw new ServicioNoDisponibleException(
                    "El menú no devolvió la cotización de los modificadores");
        }
        List<ModificadorElegido> salida = lista(cotizacion.get("elegidos")).stream()
                .map(m -> new ModificadorElegido(uuid(m.get("id")), texto(m.get("nombre")),
                        decimal(m.get("precioExtra"))))
                .toList();
        return new Cotizacion(salida, decimal(cotizacion.get("extraTotal")));
    }

    /** Los errores propios se dejan pasar; cualquier otro fallo es "el menú no responde". */
    private static <T> T preguntar(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (ErrorDeAplicacion propio) {
            throw propio;
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar servicio-menu: {}", fallo.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar el menú: intenta de nuevo en un momento");
        }
    }

    /** El 404 y el 422 del menú significan lo mismo acá; el resto, que no se sabe. */
    private static void traducir(ClientHttpResponse respuesta) throws java.io.IOException {
        HttpStatus estado = HttpStatus.resolve(respuesta.getStatusCode().value());
        if (estado == HttpStatus.NOT_FOUND) {
            throw new NoEncontradoException("Ese ítem o modificador no existe en el menú");
        }
        if (estado == HttpStatus.UNPROCESSABLE_ENTITY) {
            throw new ReglaDeNegocioException(detalle(respuesta,
                    "La selección de modificadores no es válida"));
        }
        if (estado == HttpStatus.FORBIDDEN) {
            throw new SinPermisoException("MENU_PLATO_VER");
        }
        throw new ServicioNoDisponibleException(
                "El menú respondió con un error (" + respuesta.getStatusCode().value() + ")");
    }

    private static String detalle(ClientHttpResponse respuesta, String porDefecto) {
        try {
            Object detalle = JSON.readValue(respuesta.getBody(), Map.class).get("detail");
            return detalle == null ? porDefecto : detalle.toString();
        } catch (java.io.IOException | RuntimeException ilegible) {
            return porDefecto;
        }
    }

    private static void conLaIdentidadDeQuienPide(HttpHeaders cabeceras, UUID negocioId) {
        DatosDelNegocio quien = ContextoDeNegocio.actual();
        cabeceras.set(CabecerasDeNegocio.NEGOCIO, negocioId.toString());
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
    private static List<Map<String, Object>> lista(Object valor) {
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }
}
