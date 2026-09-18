package com.regenta.reportes.infra;

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
import com.regenta.reportes.aplicacion.AgregadorDiario;

/**
 * Ajusta el agregado diario cuando una venta se anula (HU-097 criterio 4). No
 * toca {@code hechos_venta} —esa tabla es el histórico de lo que pasó, no se
 * reescribe— solo revierte lo que {@link AgregadorDiario} había sumado al
 * cerrar la venta, usando lo que quedó registrado en
 * {@code agregados_diarios_documento}.
 *
 * <p>Payload esperado de {@code venta_anulada} (HU-041): {@code {negocio_id,
 * venta_id, numero, motivo, lineas:[...]}}.
 */
@Component
public class ConsumidorDeVentasAnuladas {

    private final InboxIdempotente inbox;
    private final AgregadorDiario agregador;
    private final ObjectMapper json;

    public ConsumidorDeVentasAnuladas(InboxIdempotente inbox, AgregadorDiario agregador, ObjectMapper json) {
        this.inbox = inbox;
        this.agregador = agregador;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reportes.ventas-anuladas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"venta_anulada"}))
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
                tipoEvento, cuerpo, payload -> procesar(negocioId, datos)));
    }

    private void procesar(UUID negocioId, Map<String, Object> datos) {
        UUID ventaId = uuid(datos.get("venta_id"));
        if (ventaId == null) {
            return;
        }
        agregador.revertir(negocioId, "VENTA", ventaId);
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento venta_anulada", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }
}
