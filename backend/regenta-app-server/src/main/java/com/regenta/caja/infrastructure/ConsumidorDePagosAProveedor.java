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
 * HU-145 · Compras publica {@code pago_a_proveedor_registrado}; si el pago fue en efectivo desde
 * una sesión de caja abierta, aquí se registra la salida. El cajero no lo anota a mano (I13 de
 * {@code docs/dominio/caja.md}). Pasa por el Inbox: un duplicado no se procesa dos veces.
 */
@Component
public class ConsumidorDePagosAProveedor {

    static final String EVENTO = "pago_a_proveedor_registrado";
    /** HU-172 · El pago se anuló: el efectivo vuelve a la caja. */
    static final String EVENTO_ANULADO = "pago_a_proveedor_anulado";
    private static final String CONSUMIDOR = "caja.ConsumidorDePagosAProveedor";

    private final InboxIdempotente inbox;
    private final RegistroDeMovimientos movimientos;
    private final ObjectMapper json;

    public ConsumidorDePagosAProveedor(InboxIdempotente inbox, RegistroDeMovimientos movimientos,
            ObjectMapper json) {
        this.inbox = inbox;
        this.movimientos = movimientos;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null
                || (!EVENTO.equals(evento.tipoEvento()) && !EVENTO_ANULADO.equals(evento.tipoEvento()))) {
            return;
        }
        String tipo = evento.tipoEvento();
        String cuerpo = serializar(evento.payload());
        Map<String, Object> datos = leer(cuerpo);
        UUID negocioId = evento.negocioId();
        if (negocioId == null && datos.get("negocio_id") != null) {
            negocioId = UUID.fromString(datos.get("negocio_id").toString());
        }
        if (negocioId == null) {
            return;
        }
        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(CONSUMIDOR, evento.id(), nId,
                tipo, cuerpo, payload -> {
                    if (EVENTO_ANULADO.equals(tipo)) {
                        movimientos.reversarPagoAProveedor(datos);
                    } else {
                        movimientos.registrarPagoAProveedor(datos);
                    }
                }));
    }

    private String serializar(Object payload) {
        if (payload == null) {
            return "{}";
        }
        if (payload instanceof String s) {
            return s;
        }
        try {
            return json.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo serializar el pago a proveedor", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el pago a proveedor", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CAJA"), Set.of(), Set.of());
    }
}
