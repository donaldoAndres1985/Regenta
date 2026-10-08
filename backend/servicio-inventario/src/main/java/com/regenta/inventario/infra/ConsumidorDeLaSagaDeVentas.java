package com.regenta.inventario.infra;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.inventario.aplicacion.GestionDeReservasStock;
import com.regenta.inventario.aplicacion.LineaDeReserva;
import com.regenta.inventario.aplicacion.SolicitudDeReservaStock;

/**
 * El lado de Inventario de la saga de ventas (HU-034 / HU-038), por el bus:
 * <ul>
 * <li>{@code solicitar_reserva_stock}: aparta el stock y responde
 * {@code stock_reservado} o {@code stock_reserva_fallida};</li>
 * <li>{@code venta_completada}: convierte la reserva en salida real.</li>
 * </ul>
 * Pasa siempre por el Inbox: RabbitMQ entrega at-least-once y la misma
 * solicitud no debe reservar dos veces (HU-127 criterio 4).
 */
@Component
public class ConsumidorDeLaSagaDeVentas {

    private final InboxIdempotente inbox;
    private final GestionDeReservasStock reservas;
    private final ObjectMapper json;

    public ConsumidorDeLaSagaDeVentas(InboxIdempotente inbox, GestionDeReservasStock reservas,
            ObjectMapper json) {
        this.inbox = inbox;
        this.reservas = reservas;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "inventario.saga-de-ventas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"solicitar_reserva_stock", "venta_completada"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String tipoEvento = props.getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);
        UUID negocioId = UUID.fromString(datos.get("negocio_id").toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo, payload -> {
                    if ("venta_completada".equals(tipoEvento)) {
                        reservas.confirmar("VENTA", UUID.fromString(datos.get("venta_id").toString()));
                    } else {
                        reservas.solicitar(solicitudDe(datos));
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private static SolicitudDeReservaStock solicitudDe(Map<String, Object> datos) {
        List<LineaDeReserva> lineas = ((List<Map<String, Object>>) datos.get("lineas")).stream()
                .map(l -> new LineaDeReserva(UUID.fromString(l.get("producto_id").toString()),
                        UUID.fromString(l.get("bodega_id").toString()),
                        new BigDecimal(l.get("cantidad").toString())))
                .toList();
        return new SolicitudDeReservaStock(datos.get("origen_tipo").toString(),
                UUID.fromString(datos.get("origen_id").toString()),
                UUID.fromString(datos.get("correlacion_id").toString()), null, lineas);
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de la saga", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("INVENTARIO"), Set.of("INVENTARIO_RESERVA_GESTIONAR"), Set.of());
    }
}
