package com.regenta.facturacion.infra;

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
import com.regenta.facturacion.aplicacion.EmisorDeNegocios;

/**
 * Mantiene los datos fiscales del negocio que van en la factura (HU-115).
 *
 * <p>Los dos eventos traen la identidad completa, así que el consumidor hace
 * siempre lo mismo y no le importa cuál llegó: guarda la foto. Uno que arranque
 * a mitad de camino queda bien con el primer evento que reciba, sin reproducir
 * la historia.
 */
@Component
public class ConsumidorDeNegocio {

    private final InboxIdempotente inbox;
    private final EmisorDeNegocios emisores;
    private final ObjectMapper json;

    public ConsumidorDeNegocio(InboxIdempotente inbox, EmisorDeNegocios emisores,
            ObjectMapper json) {
        this.inbox = inbox;
        this.emisores = emisores;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "facturacion.negocio", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"negocio_creado", "configuracion_negocio_actualizada"}))
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

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(mensajeId, negocioId,
                tipoEvento, cuerpo, payload -> emisores.guardar(negocioId, datos)));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "", Set.of(), Set.of("FACTURACION"),
                Set.of(), Set.of());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento del negocio", e);
        }
    }
}
