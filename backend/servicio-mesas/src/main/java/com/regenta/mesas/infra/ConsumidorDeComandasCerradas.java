package com.regenta.mesas.infra;

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
import com.regenta.mesas.aplicacion.GestionDeSesionesDeMesa;

/**
 * Cuando se cierra la comanda de una mesa, cierra su sesión y deja la mesa
 * «por limpiar», no libre (HU-082 criterio 3). Pasa por el Inbox: la misma
 * comanda cerrada dos veces no reabre nada.
 *
 * <p>Payload esperado de {@code comanda_cerrada}:
 * {@code {negocio_id, comanda_id?, mesa_id?, sesion_id?}} (lo produce E12).
 */
@Component
public class ConsumidorDeComandasCerradas {

    private final InboxIdempotente inbox;
    private final GestionDeSesionesDeMesa sesiones;
    private final ObjectMapper json;

    public ConsumidorDeComandasCerradas(InboxIdempotente inbox, GestionDeSesionesDeMesa sesiones,
            ObjectMapper json) {
        this.inbox = inbox;
        this.sesiones = sesiones;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "mesas.comandas-cerradas", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"comanda_cerrada"}))
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
        UUID sesionId = uuid(datos.get("sesion_id"));
        UUID mesaId = uuid(datos.get("mesa_id"));
        UUID comandaId = uuid(datos.get("comanda_id"));
        if (sesionId == null && mesaId == null && comandaId == null) {
            return;
        }

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> sesiones.cerrarPorComanda(negocioId, sesionId, mesaId, comandaId)));
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
            throw new IllegalArgumentException("No se pudo leer el evento comanda_cerrada", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MESAS"),
                Set.of(), Set.of());
    }
}
