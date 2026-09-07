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
import com.regenta.facturacion.aplicacion.GestionDeFacturas;
import com.regenta.facturacion.domain.OrigenDeFactura;

/**
 * Emite la factura cuando se cierra una venta, una estancia o un pedido. El
 * mismo código para los tres: cambia solo el {@code origen_tipo} según la clave
 * de enrutamiento (criterios 1, 2 y 3). Pasa por el Inbox: una segunda entrega
 * del mismo evento no emite una segunda factura (criterio 4).
 */
@Component
public class ConsumidorDeCierresFacturables {

    private final InboxIdempotente inbox;
    private final GestionDeFacturas facturas;
    private final ObjectMapper json;

    public ConsumidorDeCierresFacturables(InboxIdempotente inbox, GestionDeFacturas facturas,
            ObjectMapper json) {
        this.inbox = inbox;
        this.facturas = facturas;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "facturacion.cierres-facturables", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"venta_completada", "estancia_finalizada", "pedido_completado"}))
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
        OrigenDeFactura origen = origenDe(tipoEvento);

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> facturas.emitirDesdeEvento(origen, datos)));
    }

    private static OrigenDeFactura origenDe(String tipoEvento) {
        return switch (tipoEvento) {
            case "estancia_finalizada" -> OrigenDeFactura.RESERVA;
            case "pedido_completado" -> OrigenDeFactura.COMANDA;
            default -> OrigenDeFactura.VENTA;
        };
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
                Set.of("FACTURACION"), Set.of(), Set.of());
    }
}
