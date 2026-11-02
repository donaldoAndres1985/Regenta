package com.regenta.alertas.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.application.VigilanciaDeInventario;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Escucha los hechos de inventario y los pasa por el motor de alertas (HU-093).
 * Pasa por el Inbox: un evento repetido no genera una segunda alerta ni cierra
 * dos veces.
 *
 * <ul>
 *   <li>{@code stock_bajo_minimo} → alerta de stock (criterio 1).</li>
 *   <li>{@code lote_por_vencer} → alerta de vencimiento (criterio 2).</li>
 *   <li>{@code stock_normalizado} → resuelve la alerta de stock (criterio 4).</li>
 * </ul>
 */
@Component
public class ConsumidorDeInventario {

    private final InboxIdempotente inbox;
    private final VigilanciaDeInventario vigilancia;
    private final ObjectMapper json;

    public ConsumidorDeInventario(InboxIdempotente inbox, VigilanciaDeInventario vigilancia,
            ObjectMapper json) {
        this.inbox = inbox;
        this.vigilancia = vigilancia;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"stock_bajo_minimo".equals(tipoEvento)
                && !"lote_por_vencer".equals(tipoEvento)
                && !"stock_normalizado".equals(tipoEvento)) {
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

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(
                mensajeId, nId, tipoEvento, cuerpo, payload -> despachar(tipoEvento, datos)));
    }

    private void despachar(String tipoEvento, Map<String, Object> datos) {
        switch (tipoEvento) {
            case "stock_bajo_minimo" -> vigilancia.alStockBajoMinimo(datos);
            case "lote_por_vencer" -> vigilancia.alLotePorVencer(datos);
            case "stock_normalizado" -> vigilancia.alStockNormalizado(datos);
            default -> { /* clave no esperada: se ignora */ }
        }
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
            throw new IllegalArgumentException("No se pudo leer el evento de inventario", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("ALERTAS"), Set.of(), Set.of());
    }
}
