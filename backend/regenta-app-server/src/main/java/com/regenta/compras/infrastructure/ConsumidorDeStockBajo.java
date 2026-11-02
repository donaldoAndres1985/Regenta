package com.regenta.compras.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.compras.application.GestionDeSugerencias;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Cuando Inventario avisa que un producto bajó del mínimo, Compras lo mete en la
 * lista de reposición (HU-050 criterio 1). Pasa por el Inbox para que el mismo
 * evento repetido no cree dos sugerencias.
 */
@Component
public class ConsumidorDeStockBajo {

    private final InboxIdempotente inbox;
    private final GestionDeSugerencias sugerencias;
    private final ObjectMapper json;

    public ConsumidorDeStockBajo(InboxIdempotente inbox, GestionDeSugerencias sugerencias,
            ObjectMapper json) {
        this.inbox = inbox;
        this.sugerencias = sugerencias;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !"stock_bajo_minimo".equals(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String tipoEvento = eventoGenerico.tipoEvento();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        Map<String, Object> datos = leer(cuerpo);

        UUID negocioId = eventoGenerico.negocioId();
        if (negocioId == null && datos.get("negocio_id") != null) {
            negocioId = UUID.fromString(datos.get("negocio_id").toString());
        }
        if (negocioId == null) {
            return;
        }

        final UUID tenantId = negocioId;
        ContextoDeNegocio.en(contextoDe(tenantId), () -> inbox.procesarUnaVez(
                mensajeId, tenantId, tipoEvento, cuerpo,
                payload -> sugerencias.registrarDesdeEvento(datos)));
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
            throw new IllegalArgumentException("No se pudo serializar el payload del evento", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento stock_bajo_minimo", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("COMPRAS"), Set.of(), Set.of());
    }
}
