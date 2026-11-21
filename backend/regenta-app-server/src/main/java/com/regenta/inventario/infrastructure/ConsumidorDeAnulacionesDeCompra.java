package com.regenta.inventario.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.inventario.application.SalidaPorAnulacionDeCompra;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-177 · Compras anuló una recepción ({@code recepcion_anulada}): sale de la bodega lo que había
 * entrado con ella, del mismo lote. Una sola vez por evento (inbox), como la entrada.
 */
@Component
public class ConsumidorDeAnulacionesDeCompra {

    static final String EVENTO = "recepcion_anulada";
    private static final String CONSUMIDOR = "inventario.ConsumidorDeAnulacionesDeCompra";

    private final InboxIdempotente inbox;
    private final SalidaPorAnulacionDeCompra salidas;
    private final ObjectMapper json;

    public ConsumidorDeAnulacionesDeCompra(InboxIdempotente inbox, SalidaPorAnulacionDeCompra salidas,
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
        UUID usuarioId = uuid(datos, "usuario_id");
        DatosDelNegocio contexto = new DatosDelNegocio(negocioId, usuarioId, "", "VENTA_DIRECTA", Set.of(),
                Set.of("INVENTARIO"), Set.of(), Set.of());

        // El payload tiene la misma forma que recepcion_registrada: se reusa su lectura.
        ContextoDeNegocio.en(contexto, () -> inbox.procesarUnaVez(CONSUMIDOR, evento.id(), negocioId, EVENTO,
                cuerpo, payload -> salidas.sacar(ConsumidorDeCompras.entradaDe(datos))));
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
            throw new IllegalArgumentException("No se pudo serializar el evento de la anulación", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("El evento de la anulación no es JSON válido", e);
        }
    }
}
