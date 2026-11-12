package com.regenta.facturacion.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.facturacion.application.GestionDeFirmaYTransmision;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * En cuanto se emite una factura (factura_emitida), la firma y la transmite (HU-055).
 */
@Component
public class ConsumidorDeFacturasEmitidas {

    private final InboxIdempotente inbox;
    private final GestionDeFirmaYTransmision firma;
    private final ObjectMapper json;

    public ConsumidorDeFacturasEmitidas(InboxIdempotente inbox, GestionDeFirmaYTransmision firma,
            ObjectMapper json) {
        this.inbox = inbox;
        this.firma = firma;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"factura_emitida".equals(tipoEvento)) {
            return;
        }

        UUID mensajeId = evento.id();
        String cuerpo = serializarPayload(evento.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        if (negocio == null && evento.negocioId() != null) {
            negocio = evento.negocioId();
        }
        Object factura = datos.get("factura_id");
        if (factura == null && evento.agregadoId() != null) {
            factura = evento.agregadoId();
        }
        if (negocio == null || factura == null) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        UUID facturaId = UUID.fromString(factura.toString());

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("facturacion.ConsumidorDeFacturasEmitidas", 
                mensajeId, nId, tipoEvento, cuerpo,
                payload -> firma.firmarYTransmitir(facturaId)));
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
            throw new IllegalArgumentException("No se pudo leer la factura emitida", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("FACTURACION"), Set.of(), Set.of());
    }
}
