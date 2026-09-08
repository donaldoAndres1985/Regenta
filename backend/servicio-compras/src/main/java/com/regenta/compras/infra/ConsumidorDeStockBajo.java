package com.regenta.compras.infra;

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
import com.regenta.compras.aplicacion.GestionDeSugerencias;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Cuando Inventario avisa que un producto bajó del mínimo, Compras lo mete en la
 * lista de reposición (HU-050 criterio 1). Pasa por el Inbox: el mismo evento
 * repetido no crea dos sugerencias.
 */
@Component
public class ConsumidorDeStockBajo {

    private final InboxIdempotente inbox;
    private final GestionDeSugerencias sugerencias;
    private final ObjectMapper json;

    public ConsumidorDeStockBajo(InboxIdempotente inbox, GestionDeSugerencias sugerencias,
            ObjectMapper json) {
        this.inbox = inbox;
        this.sugerencias = sugerencias;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "compras.stock-bajo-minimo", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = "stock_bajo_minimo"))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> sugerencias.registrarDesdeEvento(datos)));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento stock_bajo_minimo", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("COMPRAS"), Set.of(), Set.of());
    }
}
