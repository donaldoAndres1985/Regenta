package com.regenta.inventario.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.inventario.application.GestionDeReservasStock;
import com.regenta.inventario.application.LineaDeReserva;
import com.regenta.inventario.application.SolicitudDeReservaStock;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * El lado de Inventario de la saga de ventas (HU-034 / HU-038):
 * <ul>
 * <li>{@code solicitar_reserva_stock}: aparta el stock y responde
 * {@code stock_reservado} o {@code stock_reserva_fallida};</li>
 * <li>{@code venta_completada}: convierte la reserva en salida real.</li>
 * </ul>
 * Pasa siempre por el Inbox para garantizar idempotencia (HU-127 criterio 4).
 */
@Component
public class ConsumidorDeLaSagaDeVentas {

    private final InboxIdempotente inbox;
    private final GestionDeReservasStock reservas;
    private final ObjectMapper json;

    public ConsumidorDeLaSagaDeVentas(InboxIdempotente inbox, GestionDeReservasStock reservas,
            ObjectMapper json) {
        this.inbox = inbox;
        this.reservas = reservas;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null) {
            return;
        }
        String tipoEvento = eventoGenerico.tipoEvento();
        if (!"solicitar_reserva_stock".equals(tipoEvento) && !"venta_completada".equals(tipoEvento)) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        Map<String, Object> datos = leer(cuerpo);
        UUID negocioId = eventoGenerico.negocioId();
        if (negocioId == null && datos.get("negocio_id") != null) {
            negocioId = UUID.fromString(datos.get("negocio_id").toString());
        }
        if (negocioId == null) {
            return;
        }

        final UUID tenantId = negocioId;
        ContextoDeNegocio.en(contextoDe(tenantId), () -> {
            inbox.procesarUnaVez("inventario.ConsumidorDeLaSagaDeVentas", mensajeId, tenantId, tipoEvento, cuerpo,
                    payload -> {
                        if ("venta_completada".equals(tipoEvento)) {
                            Object ventaIdObj = datos.get("venta_id") != null
                                    ? datos.get("venta_id") : eventoGenerico.agregadoId();
                            reservas.confirmar("VENTA", UUID.fromString(ventaIdObj.toString()));
                        } else {
                            reservas.solicitar(solicitudDe(datos));
                        }
                    });
        });
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

    @SuppressWarnings("unchecked")
    private static SolicitudDeReservaStock solicitudDe(Map<String, Object> datos) {
        List<LineaDeReserva> lineas = ((List<Map<String, Object>>) datos.get("lineas")).stream()
                .map(l -> new LineaDeReserva(UUID.fromString(l.get("producto_id").toString()),
                        UUID.fromString(l.get("bodega_id").toString()),
                        new BigDecimal(l.get("cantidad").toString())))
                .toList();
        return new SolicitudDeReservaStock(datos.get("origen_tipo").toString(),
                UUID.fromString(datos.get("origen_id").toString()),
                UUID.fromString(datos.get("correlacion_id").toString()), null, lineas);
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de la saga", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("INVENTARIO"), Set.of("INVENTARIO_RESERVA_GESTIONAR"), Set.of());
    }
}
