package com.regenta.alertas.infra;

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
import com.regenta.alertas.aplicacion.VigilanciaDeAuditoria;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Escucha lo que sale mal con las operaciones hechas sin señal: el conflicto
 * de sincronización que lleva demasiado sin resolver (HU-103 criterio 5) y la
 * venta offline que chocó al subir (HU-043 criterio 4). Pasa por el Inbox: el
 * mismo evento entregado dos veces no genera una segunda alerta.
 */
@Component
public class ConsumidorDeAuditoria {

    private final InboxIdempotente inbox;
    private final VigilanciaDeAuditoria vigilancia;
    private final ObjectMapper json;

    public ConsumidorDeAuditoria(InboxIdempotente inbox, VigilanciaDeAuditoria vigilancia,
            ObjectMapper json) {
        this.inbox = inbox;
        this.vigilancia = vigilancia;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "alertas.conflictos-de-sincronizacion", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"conflicto_sync_vencido", "venta_offline_en_conflicto"}))
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
                mensajeId, negocioId, tipoEvento, cuerpo, payload -> despachar(tipoEvento, datos)));
    }

    private void despachar(String tipoEvento, Map<String, Object> datos) {
        switch (tipoEvento) {
            case "conflicto_sync_vencido" -> vigilancia.alConflictoSinResolver(datos);
            case "venta_offline_en_conflicto" -> vigilancia.alVentaOfflineEnConflicto(datos);
            default -> { /* clave no esperada: se ignora */ }
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento conflicto_sync_vencido", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("ALERTAS"), Set.of(), Set.of());
    }
}
