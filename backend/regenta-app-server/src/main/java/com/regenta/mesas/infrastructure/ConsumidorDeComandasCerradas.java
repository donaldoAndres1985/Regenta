package com.regenta.mesas.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.mesas.application.GestionDeSesionesDeMesa;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Cuando se cierra la comanda de una mesa, cierra su sesión y deja la mesa
 * «por limpiar», no libre (HU-082 criterio 3). Pasa por el Inbox.
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

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !"comanda_cerrada".equals(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String tipoEvento = eventoGenerico.tipoEvento();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null) {
            negocio = eventoGenerico.negocioId();
        }
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
            throw new IllegalArgumentException("No se pudo serializar el payload", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento comanda_cerrada", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MESAS"),
                Set.of(), Set.of());
    }
}
