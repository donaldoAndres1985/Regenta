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
import com.regenta.auditoria.domain.ConflictoSync;
import com.regenta.auditoria.domain.OperacionSync;
import com.regenta.comun.eventos.InboxIdempotente;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Recibe cómo le fue a una operación en el servicio que la aplicó de verdad
 * (HU-102 criterio 4). Una operación RECHAZADA no toca ninguna otra: cada una
 * es su propia fila, no hay una cadena que bloquear.
 *
 * <p>Cuando el resultado es CONFLICTO, además registra el conflicto (HU-103
 * criterio 1) con la versión y los datos del servidor y del cliente lado a
 * lado (criterio 2): quien detecta el conflicto de verdad es el servicio
 * dueño de la entidad —él conoce su versión actual—, aquí solo se guarda.
 *
 * <p>Payload esperado de {@code operacion_sync_resultado}: {@code {negocio_id,
 * operacion_id, resultado: 'APLICADA'|'RECHAZADA'|'CONFLICTO', motivo,
 * conflicto: {tipo, version_servidor, version_cliente, datos_servidor,
 * datos_cliente}}}. {@code conflicto} solo aplica cuando resultado es
 * CONFLICTO; {@code tipo} es uno de VERSION_DESACTUALIZADA,
 * ELIMINADO_EN_SERVIDOR, DUPLICADO, STOCK_INSUFICIENTE o REGLA_NEGOCIO
 * (HU-103 criterio 4).
 */
@Component
public class ConsumidorDeResultadoDeOperacion {

    private final InboxIdempotente inbox;
    private final OperacionSyncRepositorio operaciones;
    private final ConflictoSyncRepositorio conflictos;
    private final ObjectMapper json;

    public ConsumidorDeResultadoDeOperacion(InboxIdempotente inbox, OperacionSyncRepositorio operaciones,
            ConflictoSyncRepositorio conflictos, ObjectMapper json) {
        this.inbox = inbox;
        this.operaciones = operaciones;
        this.conflictos = conflictos;
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
            registrarConflicto(negocioId, operacion, datos, ahora);
        } else {
            operacion.aplicar(ahora);
        }
        operaciones.save(operacion);
    }

    /** HU-103 criterio 1: se registra el conflicto en vez de sobrescribir. */
    @SuppressWarnings("unchecked")
    private void registrarConflicto(UUID negocioId, OperacionSync operacion, Map<String, Object> datos,
            OffsetDateTime ahora) {
        Map<String, Object> detalle = (Map<String, Object>) datos.getOrDefault("conflicto", Map.of());
        String tipo = (String) detalle.getOrDefault("tipo", "VERSION_DESACTUALIZADA");
        Long versionServidor = numero(detalle.get("version_servidor"));
        Long versionCliente = numero(detalle.get("version_cliente"));

        conflictos.save(ConflictoSync.detectado(negocioId, operacion.getId(), operacion.getEntidadTipo(),
                operacion.getEntidadId(), tipo, versionServidor, versionCliente,
                aJson(detalle.get("datos_servidor")), aJson(detalle.get("datos_cliente")), ahora));
    }

    private static Long numero(Object valor) {
        return valor == null ? null : Long.valueOf(valor.toString());
    }

    private String aJson(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return json.writeValueAsString(valor);
        } catch (Exception e) {
            return null;
        }
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
