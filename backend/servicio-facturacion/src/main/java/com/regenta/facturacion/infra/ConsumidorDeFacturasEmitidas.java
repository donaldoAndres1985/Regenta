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
import com.regenta.facturacion.aplicacion.GestionDeFirmaYTransmision;

/**
 * En cuanto se emite una factura ({@code factura_emitida}), la firma y la
 * transmite (HU-055). Pasa por el Inbox: una segunda entrega no vuelve a firmar
 * (la firma ya es idempotente, pero el Inbox evita el trabajo repetido).
 */
@Component
public class ConsumidorDeFacturasEmitidas {

    private final InboxIdempotente inbox;
    private final GestionDeFirmaYTransmision firma;
    private final ObjectMapper json;

    public ConsumidorDeFacturasEmitidas(InboxIdempotente inbox, GestionDeFirmaYTransmision firma,
            ObjectMapper json) {
        this.inbox = inbox;
        this.firma = firma;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "facturacion.firma-y-transmision", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"factura_emitida"}))
    public void recibir(Message mensaje) {
        var props = mensaje.getMessageProperties();
        UUID mensajeId = UUID.fromString(props.getMessageId());
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        Object factura = datos.get("factura_id");
        if (negocio == null || factura == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        UUID facturaId = UUID.fromString(factura.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, "factura_emitida", cuerpo,
                payload -> firma.firmarYTransmitir(facturaId)));
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer factura_emitida", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("FACTURACION"), Set.of(), Set.of());
    }
}
