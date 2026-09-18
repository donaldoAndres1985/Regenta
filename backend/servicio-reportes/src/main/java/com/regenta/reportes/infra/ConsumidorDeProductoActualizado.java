package com.regenta.reportes.infra;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.reportes.aplicacion.ResolverDeDimensiones;

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

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.producto-actualizado", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"producto_actualizado"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        Object producto = datos.get("producto_id");
        if (negocio == null || producto == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(mensajeId, negocioId,
                tipoEvento, cuerpo, payload -> procesar(negocioId, UUID.fromString(producto.toString()), datos)));
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
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
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
