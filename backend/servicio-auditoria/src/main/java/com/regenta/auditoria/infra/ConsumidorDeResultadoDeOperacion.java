package com.regenta.auditoria.infra;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
import com.regenta.auditoria.domain.OperacionSync;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Recibe cómo le fue a una operación en el servicio que la aplicó de verdad
 * (HU-102 criterio 4). Una operación RECHAZADA no toca ninguna otra: cada una
 * es su propia fila, no hay una cadena que bloquear.
 *
 * <p>Payload esperado de {@code operacion_sync_resultado}: {@code {negocio_id,
 * operacion_id, resultado: 'APLICADA'|'RECHAZADA'|'CONFLICTO', motivo}}.
 */
@Component
public class ConsumidorDeResultadoDeOperacion {

    private final InboxIdempotente inbox;
    private final OperacionSyncRepositorio operaciones;
    private final ObjectMapper json;

    public ConsumidorDeResultadoDeOperacion(InboxIdempotente inbox, OperacionSyncRepositorio operaciones,
            ObjectMapper json) {
        this.inbox = inbox;
        this.operaciones = operaciones;
        this.json = json;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "auditoria.resultados-de-operacion", durable = "true"),
            exchange = @Exchange(name = "${regenta.eventos.exchange:regenta.eventos}",
                    type = ExchangeTypes.TOPIC, durable = "true"),
            key = {"operacion_sync_resultado"}))
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
        UUID operacionId = uuid(datos.get("operacion_id"));
        if (operacionId == null) {
            return;
        }
        OperacionSync operacion = operaciones.findById(operacionId).orElse(null);
        if (operacion == null) {
            return;
        }
        String resultado = (String) datos.get("resultado");
        String motivo = (String) datos.get("motivo");
        OffsetDateTime ahora = OffsetDateTime.now();

        if ("RECHAZADA".equals(resultado)) {
            operacion.rechazar(motivo, ahora);
        } else if ("CONFLICTO".equals(resultado)) {
            operacion.marcarConflicto(motivo, ahora);
        } else {
            operacion.aplicar(ahora);
        }
        operaciones.save(operacion);
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
            throw new IllegalArgumentException("No se pudo leer el evento operacion_sync_resultado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("AUDITORIA"),
                Set.of(), Set.of());
    }
}
