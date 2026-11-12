package com.regenta.alertas.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.application.VigilanciaDeAuditoria;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

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

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"conflicto_sync_vencido".equals(tipoEvento)
                && !"venta_offline_en_conflicto".equals(tipoEvento)) {
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
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("alertas.ConsumidorDeAuditoria", 
                mensajeId, nId, tipoEvento, cuerpo, payload -> despachar(tipoEvento, datos)));
    }

    private void despachar(String tipoEvento, Map<String, Object> datos) {
        switch (tipoEvento) {
            case "conflicto_sync_vencido" -> vigilancia.alConflictoSinResolver(datos);
            case "venta_offline_en_conflicto" -> vigilancia.alVentaOfflineEnConflicto(datos);
            default -> { /* clave no esperada: se ignora */ }
        }
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

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento conflicto_sync_vencido", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("ALERTAS"), Set.of(), Set.of());
    }
}
