package com.regenta.caja.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.caja.application.RegistroDeMovimientos;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Todo cobro entra a la caja sin importar de qué patrón viene (HU-060): venta
 * directa, comanda o reserva. La clave de enrutamiento decide el origen_tipo.
 */
@Component
public class ConsumidorDeCobros {

    private final InboxIdempotente inbox;
    private final RegistroDeMovimientos movimientos;
    private final ObjectMapper json;

    public ConsumidorDeCobros(InboxIdempotente inbox, RegistroDeMovimientos movimientos,
            ObjectMapper json) {
        this.inbox = inbox;
        this.movimientos = movimientos;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"venta_completada".equals(tipoEvento)
                && !"pedido_completado".equals(tipoEvento)
                && !"anticipo_reserva_cobrado".equals(tipoEvento)) {
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
        String origenTipo = origenDe(tipoEvento);

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("caja.ConsumidorDeCobros", 
                mensajeId, nId, tipoEvento, cuerpo,
                payload -> movimientos.registrarDesdeCobro(origenTipo, datos)));
    }

    private static String origenDe(String tipoEvento) {
        return switch (tipoEvento) {
            case "pedido_completado" -> "COMANDA";
            case "anticipo_reserva_cobrado" -> "RESERVA";
            default -> "VENTA";
        };
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
            throw new IllegalArgumentException("No se pudo leer el evento de cobro", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CAJA"), Set.of(), Set.of());
    }
}
