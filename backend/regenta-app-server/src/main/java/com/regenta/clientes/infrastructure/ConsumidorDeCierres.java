package com.regenta.clientes.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.clientes.application.EventoDeCompra;
import com.regenta.clientes.application.ProyeccionDeMetricas;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Alimenta {@code cliente_metricas} con los eventos de cierre de los tres
 * patrones (HU-023): {@code venta_completada}, {@code estancia_finalizada} y
 * {@code pedido_completado} suman; {@code venta_anulada} resta. Pasa siempre por
 * el Inbox: no debe contarse dos veces.
 */
@Component
public class ConsumidorDeCierres {

    private static final String ANULACION = "venta_anulada";
    private static final Set<String> EVENTOS_CIERRE = Set.of(
            "venta_completada", "estancia_finalizada", "pedido_completado", "venta_anulada"
    );

    private final InboxIdempotente inbox;
    private final ProyeccionDeMetricas proyeccion;
    private final ObjectMapper json;

    public ConsumidorDeCierres(InboxIdempotente inbox, ProyeccionDeMetricas proyeccion,
            ObjectMapper json) {
        this.inbox = inbox;
        this.proyeccion = proyeccion;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !EVENTOS_CIERRE.contains(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String tipoEvento = eventoGenerico.tipoEvento();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        EventoDeCompra evento = EventoDeCompra.desde(leer(cuerpo));

        if (evento.negocioId() == null) {
            return;   // evento sin negocio: no es de este servicio
        }

        ContextoDeNegocio.en(contextoDe(evento.negocioId()), () -> inbox.procesarUnaVez("clientes.ConsumidorDeCierres", 
                mensajeId, evento.negocioId(), tipoEvento, cuerpo, payload -> {
                    if (ANULACION.equals(tipoEvento)) {
                        proyeccion.revertirCompra(evento);
                    } else {
                        proyeccion.registrarCompra(evento);
                    }
                }));
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
            @SuppressWarnings("unchecked")
            Map<String, Object> mapa = json.readValue(cuerpo, Map.class);
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de cierre", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CLIENTES"), Set.of(), Set.of());
    }
}
