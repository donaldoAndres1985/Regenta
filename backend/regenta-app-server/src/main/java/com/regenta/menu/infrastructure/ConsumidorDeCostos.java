package com.regenta.menu.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.menu.application.GestionDeRecetas;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Cuando cambia el costo de un insumo en inventario, recalcula el costo estimado
 * de los ítems cuya receta lo usa (HU-079 criterio 2). Pasa por el Inbox.
 */
@Component
public class ConsumidorDeCostos {

    private final InboxIdempotente inbox;
    private final GestionDeRecetas recetas;
    private final ObjectMapper json;

    public ConsumidorDeCostos(InboxIdempotente inbox, GestionDeRecetas recetas, ObjectMapper json) {
        this.inbox = inbox;
        this.recetas = recetas;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !"costo_producto_actualizado".equals(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String tipoEvento = eventoGenerico.tipoEvento();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        Object producto = datos.get("producto_id");
        if (negocio == null || producto == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        UUID productoId = UUID.fromString(producto.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez(
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> recetas.recalcularItemsQueUsan(negocioId, productoId)));
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
            throw new IllegalArgumentException("No se pudo leer costo_producto_actualizado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }
}
