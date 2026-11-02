package com.regenta.facturacion.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.facturacion.application.GestionDeFacturas;
import com.regenta.facturacion.domain.OrigenDeFactura;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Emite la factura cuando se cierra una venta, una estancia o un pedido.
 */
@Component
public class ConsumidorDeCierresFacturables {

    private final InboxIdempotente inbox;
    private final GestionDeFacturas facturas;
    private final ObjectMapper json;

    public ConsumidorDeCierresFacturables(InboxIdempotente inbox, GestionDeFacturas facturas,
            ObjectMapper json) {
        this.inbox = inbox;
        this.facturas = facturas;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"venta_completada".equals(tipoEvento)
                && !"estancia_finalizada".equals(tipoEvento)
                && !"pedido_completado".equals(tipoEvento)) {
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
        OrigenDeFactura origen = switch (tipoEvento) {
            case "estancia_finalizada" -> OrigenDeFactura.RESERVA;
            case "pedido_completado" -> OrigenDeFactura.COMANDA;
            default -> OrigenDeFactura.VENTA;
        };

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(
                mensajeId, nId, tipoEvento, cuerpo,
                payload -> facturas.emitirDesdeEvento(origen, datos)));
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
            throw new IllegalArgumentException("No se pudo leer el evento facturable", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("FACTURACION"), Set.of(), Set.of());
    }
}
