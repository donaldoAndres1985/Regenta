package com.regenta.reportes.infrastructure;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.regenta.shared.events.EventoGenerico;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.reportes.application.ResolverDeDimensiones;

/**
 * Versiona {@code dim_producto} (SCD tipo 2, HU-096 criterio 2): cuando cambia
 * el dato maestro de un producto, cierra la versión actual y abre una nueva.
 * Los hechos ya insertados con la versión anterior no se tocan (criterio 3).
 *
 * <p>Payload esperado de {@code producto_actualizado}: {@code {negocio_id,
 * producto_id, sku, nombre, categoria_id, categoria_nombre, precio_venta,
 * costo}}.
 */
@Component
public class ConsumidorDeProductoActualizado {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final ObjectMapper json;

    public ConsumidorDeProductoActualizado(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("producto_actualizado");

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!EVENTOS_SOPORTADOS.contains(tipoEvento)) {
            return;
        }

        UUID mensajeId = evento.id();
        String cuerpo = serializarPayload(evento.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null && evento.negocioId() != null) {
            negocio = evento.negocioId();
        }
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        final UUID nId = negocioId;

        Object producto = datos.get("producto_id"); if (producto == null) return; UUID prodId = UUID.fromString(producto.toString()); ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("reportes.ConsumidorDeProductoActualizado", mensajeId, nId, tipoEvento, cuerpo, payload -> procesar(nId, prodId, datos)));
    }

    private String serializarPayload(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void procesar(UUID negocioId, UUID productoId, Map<String, Object> datos) {
        String sku = (String) datos.get("sku");
        String nombre = (String) datos.get("nombre");
        UUID categoriaId = datos.get("categoria_id") == null ? null
                : UUID.fromString(datos.get("categoria_id").toString());
        String categoriaNombre = (String) datos.get("categoria_nombre");
        BigDecimal precioVenta = numero(datos.get("precio_venta"));
        BigDecimal costo = numero(datos.get("costo"));

        dimensiones.productoSkDesdeCatalogo(negocioId, productoId, sku, nombre, categoriaId,
                categoriaNombre, precioVenta, costo);
    }

    private static BigDecimal numero(Object valor) {
        return valor == null ? null : new BigDecimal(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento producto_actualizado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }
}
