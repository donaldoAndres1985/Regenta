package com.regenta.menu.infra;

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
import com.regenta.menu.aplicacion.GestionDeRecetas;

/**
 * Cuando cambia el costo de un insumo en inventario, recalcula el costo estimado
 * de los ítems cuya receta lo usa (HU-079 criterio 2). Pasa por el Inbox.
 *
 * <p>Payload esperado de {@code costo_producto_actualizado}:
 * {@code {negocio_id, producto_id}}.
 */
@Component
public class ConsumidorDeCostos {

    private final InboxIdempotente inbox;
    private final GestionDeRecetas recetas;
    private final ObjectMapper json;

    public ConsumidorDeCostos(InboxIdempotente inbox, GestionDeRecetas recetas, ObjectMapper json) {
        this.inbox = inbox;
        this.recetas = recetas;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "menu.costos-de-insumo", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"costo_producto_actualizado"}))
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
        UUID productoId = UUID.fromString(producto.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> recetas.recalcularItemsQueUsan(negocioId, productoId)));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer costo_producto_actualizado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }
}
