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
import com.regenta.clientes.aplicacion.GestionDeCartera;
import com.regenta.clientes.aplicacion.GestionDeCartera.EventoDeCredito;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Abre la cuenta por cobrar cuando Ventas cierra una venta a crédito
 * ({@code venta_a_credito}). Pasa por el Inbox: una venta no debe generar dos
 * cuentas si el evento llega repetido.
 */
@Component
public class ConsumidorDeVentasACredito {

    private final InboxIdempotente inbox;
    private final GestionDeCartera cartera;
    private final ObjectMapper json;

    public ConsumidorDeVentasACredito(InboxIdempotente inbox, GestionDeCartera cartera,
            ObjectMapper json) {
        this.inbox = inbox;
        this.cartera = cartera;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "clientes.ventas-a-credito", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"venta_a_credito"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        EventoDeCredito evento = EventoDeCredito.desde(leer(cuerpo));

        if (evento.negocioId() == null || evento.clienteId() == null) {
            return;
        }

        ContextoDeNegocio.en(contextoDe(evento.negocioId()), () -> inbox.procesarUnaVez(
                mensajeId, evento.negocioId(), "venta_a_credito", cuerpo,
                payload -> cartera.abrirCuentaPorVentaACredito(evento)));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer venta_a_credito", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CLIENTES"), Set.of(), Set.of());
    }
}
