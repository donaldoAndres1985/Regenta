package com.regenta.auditoria.infrastructure;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.auditoria.domain.ConflictoSync;
import com.regenta.auditoria.domain.OperacionSync;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Recibe cómo le fue a una operación en el servicio que la aplicó de verdad
 * (HU-102 criterio 4).
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

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"operacion_sync_resultado".equals(tipoEvento)) {
            return;
        }

        UUID mensajeId = evento.id();
        String cuerpo = serializarPayload(evento.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null && evento.negocioId() != null) {
            negocio = evento.negocioId();
        }
        if (negocio == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("auditoria.ConsumidorDeResultadoDeOperacion", mensajeId, nId,
                tipoEvento, cuerpo, payload -> procesar(nId, datos)));
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

    private String serializarPayload(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
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
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento operacion_sync_resultado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("AUDITORIA"),
                Set.of(), Set.of());
    }
}
