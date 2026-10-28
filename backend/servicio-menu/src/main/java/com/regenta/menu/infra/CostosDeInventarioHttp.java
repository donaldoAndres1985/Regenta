package com.regenta.menu.infra;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.ErrorDeAplicacion;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.comun.negocio.IdentidadSaliente;
import com.regenta.menu.aplicacion.CostosDeInventario;

/**
 * Le pregunta a servicio-inventario por el último costo de entrada de los
 * insumos de una receta (HU-079, HU-138 criterio 6).
 *
 * <p>Con un administrador detrás va con su identidad; al recalcular por
 * {@code costo_producto_actualizado} no hay nadie, y va como SISTEMA con solo
 * {@code INVENTARIO_PRODUCTO_VER} ({@link IdentidadSaliente}). Si inventario no
 * responde, se corta con 503: valorar la receta en cero borraría un costo que
 * sí existe (criterio 10).
 */
@Component
public class CostosDeInventarioHttp implements CostosDeInventario {

    private static final Logger log = LoggerFactory.getLogger(CostosDeInventarioHttp.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public CostosDeInventarioHttp(RestClient.Builder builder,
            @Value("${regenta.inventario.url:http://localhost:8083}") String urlDeInventario) {
        this.http = builder.baseUrl(urlDeInventario).build();
    }

    @Override
    public Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> costos;
        try {
            costos = http.get()
                    .uri(u -> u.path("/api/inventario/productos/costos")
                            .queryParam("ids", productoIds.toArray()).build())
                    .headers(c -> IdentidadSaliente.aplicar(c, negocioId, "INVENTARIO",
                            "INVENTARIO_PRODUCTO_VER"))
                    .retrieve()
                    .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                    .body(LISTA);
        } catch (ErrorDeAplicacion propio) {
            throw propio;
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar servicio-inventario: {}", fallo.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudieron consultar los costos en inventario: intenta de nuevo en un momento");
        }
        if (costos == null) {
            throw new ServicioNoDisponibleException("Los costos de inventario llegaron vacíos");
        }
        Map<UUID, BigDecimal> porProducto = new LinkedHashMap<>();
        for (Map<String, Object> c : costos) {
            porProducto.put(UUID.fromString(c.get("productoId").toString()),
                    new BigDecimal(c.get("costoUnitario").toString()));
        }
        return porProducto;
    }

    private static void traducir(ClientHttpResponse respuesta) throws java.io.IOException {
        if (HttpStatus.resolve(respuesta.getStatusCode().value()) == HttpStatus.FORBIDDEN) {
            throw new SinPermisoException("INVENTARIO_PRODUCTO_VER");
        }
        throw new ServicioNoDisponibleException("Los costos de inventario respondieron con un error ("
                + respuesta.getStatusCode().value() + ")");
    }
}
