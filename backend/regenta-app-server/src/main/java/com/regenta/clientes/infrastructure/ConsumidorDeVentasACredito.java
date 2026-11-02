package com.regenta.clientes.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.clientes.application.GestionDeCartera;
import com.regenta.clientes.application.GestionDeCartera.EventoDeCredito;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Abre la cuenta por cobrar cuando Ventas cierra una venta a crédito
 * ({@code venta_a_credito}). Pasa por el Inbox: una venta no debe generar dos
 * cuentas si el evento llega repetido.
 */
@Component
public class ConsumidorDeVentasACredito {

    private final InboxIdempotente inbox;
    private final GestionDeCartera cartera;
    private final ObjectMapper json;

    public ConsumidorDeVentasACredito(InboxIdempotente inbox, GestionDeCartera cartera,
            ObjectMapper json) {
        this.inbox = inbox;
        this.cartera = cartera;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !"venta_a_credito".equals(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        EventoDeCredito evento = EventoDeCredito.desde(leer(cuerpo));

        if (evento.negocioId() == null || evento.clienteId() == null) {
            return;
        }

        ContextoDeNegocio.en(contextoDe(evento.negocioId()), () -> inbox.procesarUnaVez(
                mensajeId, evento.negocioId(), "venta_a_credito", cuerpo,
                payload -> cartera.abrirCuentaPorVentaACredito(evento)));
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
            throw new IllegalArgumentException("No se pudo leer venta_a_credito", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CLIENTES"), Set.of(), Set.of());
    }
}
