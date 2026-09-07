package com.regenta.clientes.infra;

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
import com.regenta.clientes.aplicacion.EventoDeCompra;
import com.regenta.clientes.aplicacion.ProyeccionDeMetricas;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Alimenta {@code cliente_metricas} con los eventos de cierre de los tres
 * patrones (HU-023): {@code venta_completada}, {@code estancia_finalizada} y
 * {@code pedido_completado} suman; {@code venta_anulada} resta. Pasa siempre por
 * el Inbox: RabbitMQ entrega at-least-once y una compra no debe contarse dos
 * veces.
 */
@Component
public class ConsumidorDeCierres {

    private static final String ANULACION = "venta_anulada";

    private final InboxIdempotente inbox;
    private final ProyeccionDeMetricas proyeccion;
    private final ObjectMapper json;

    public ConsumidorDeCierres(InboxIdempotente inbox, ProyeccionDeMetricas proyeccion,
            ObjectMapper json) {
        this.inbox = inbox;
        this.proyeccion = proyeccion;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "clientes.cierres-de-venta", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"venta_completada", "estancia_finalizada", "pedido_completado", "venta_anulada"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        EventoDeCompra evento = EventoDeCompra.desde(leer(cuerpo));

        if (evento.negocioId() == null) {
            return;   // evento sin negocio: no es de este servicio
        }

        ContextoDeNegocio.en(contextoDe(evento.negocioId()), () -> inbox.procesarUnaVez(
                mensajeId, evento.negocioId(), tipoEvento, cuerpo, payload -> {
                    if (ANULACION.equals(tipoEvento)) {
                        proyeccion.revertirCompra(evento);
                    } else {
                        proyeccion.registrarCompra(evento);
                    }
                }));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de cierre", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CLIENTES"), Set.of(), Set.of());
    }
}
