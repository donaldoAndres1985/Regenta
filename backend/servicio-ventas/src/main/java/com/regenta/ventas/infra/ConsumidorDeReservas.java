package com.regenta.ventas.infra;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.regenta.ventas.aplicacion.SagaDeConfirmacionDeVenta;

/**
 * Recibe de Inventario la respuesta a la reserva de stock y hace avanzar la
 * saga (HU-038). Pasa siempre por el Inbox: RabbitMQ entrega at-least-once y el
 * mismo evento no debe confirmar la venta dos veces.
 */
@Component
public class ConsumidorDeReservas {

    private final InboxIdempotente inbox;
    private final SagaDeConfirmacionDeVenta saga;
    private final ObjectMapper json;

    public ConsumidorDeReservas(InboxIdempotente inbox, SagaDeConfirmacionDeVenta saga,
            ObjectMapper json) {
        this.inbox = inbox;
        this.saga = saga;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "ventas.reservas-de-stock", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"stock_reservado", "stock_reserva_fallida"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        UUID negocioId = UUID.fromString((String) datos.get("negocio_id"));
        UUID correlacionId = UUID.fromString((String) datos.get("correlacion_id"));

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo, payload -> {
                    if ("stock_reserva_fallida".equals(tipoEvento)) {
                        saga.alReservaFallida(correlacionId, motivoDe(datos));
                    } else {
                        saga.alStockReservado(correlacionId);
                    }
                }));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de reserva", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String motivoDe(Map<String, Object> datos) {
        Object faltantes = datos.get("faltantes");
        if (!(faltantes instanceof List<?> lista) || lista.isEmpty()) {
            return "no hay stock suficiente";
        }
        return lista.stream()
                .map(f -> (Map<String, Object>) f)
                .map(f -> "producto " + f.get("producto_id") + ": faltan "
                        + f.get("solicitado") + " y hay " + f.get("disponible"))
                .collect(Collectors.joining("; "));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("VENTAS"), Set.of(), Set.of());
    }
}
