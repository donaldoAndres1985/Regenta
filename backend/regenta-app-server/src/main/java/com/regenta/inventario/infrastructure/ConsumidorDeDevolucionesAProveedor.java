package com.regenta.inventario.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.inventario.application.EntradaDeRecepcion;
import com.regenta.inventario.application.SalidaPorAnulacionDeCompra;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-178 · Compras registró una devolución al proveedor ({@code devolucion_proveedor_registrada}):
 * lo devuelto sale de la bodega, del lote elegido. Una sola vez por evento (inbox).
 */
@Component
public class ConsumidorDeDevolucionesAProveedor {

    static final String EVENTO = "devolucion_proveedor_registrada";
    private static final String CONSUMIDOR = "inventario.ConsumidorDeDevolucionesAProveedor";

    private final InboxIdempotente inbox;
    private final SalidaPorAnulacionDeCompra salidas;
    private final ObjectMapper json;

    public ConsumidorDeDevolucionesAProveedor(InboxIdempotente inbox, SalidaPorAnulacionDeCompra salidas,
            ObjectMapper json) {
        this.inbox = inbox;
        this.salidas = salidas;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null || !EVENTO.equals(evento.tipoEvento())) {
            return;
        }
        String cuerpo = serializar(evento.payload());
        Map<String, Object> datos = leer(cuerpo);
        UUID negocioId = evento.negocioId() != null ? evento.negocioId() : uuid(datos, "negocio_id");
        if (negocioId == null) {
            return;
        }
        DatosDelNegocio contexto = new DatosDelNegocio(negocioId, uuid(datos, "usuario_id"), "", "VENTA_DIRECTA",
                Set.of(), Set.of("INVENTARIO"), Set.of(), Set.of());
        ContextoDeNegocio.en(contexto, () -> inbox.procesarUnaVez(CONSUMIDOR, evento.id(), negocioId, EVENTO,
                cuerpo, payload -> salidas.devolverAlProveedor(salidaDe(datos))));
    }

    /** El payload tiene la forma de {@code recepcion_registrada}, con el id de la devolución. */
    static EntradaDeRecepcion salidaDe(Map<String, Object> datos) {
        EntradaDeRecepcion leida = ConsumidorDeCompras.entradaDe(datos);
        return new EntradaDeRecepcion(uuid(datos, "devolucion_id"), leida.numero(), leida.bodegaId(),
                leida.lineas());
    }

    private static UUID uuid(Map<String, Object> datos, String campo) {
        Object v = datos.get(campo);
        return v == null ? null : UUID.fromString(v.toString());
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
            throw new IllegalArgumentException("No se pudo serializar el evento de la devolución", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("El evento de la devolución no es JSON válido", e);
        }
    }
}
