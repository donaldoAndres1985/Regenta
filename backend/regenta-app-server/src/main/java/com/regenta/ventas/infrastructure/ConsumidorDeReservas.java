package com.regenta.ventas.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.ventas.application.SagaDeConfirmacionDeVenta;

/**
 * Recibe de Inventario la respuesta a la reserva de stock y hace avanzar la saga (HU-038).
 */
@Component
public class ConsumidorDeReservas {

    // DIAGNOSTICO TEMPORAL: confirmar si este listener de verdad recibe algo.
    // Quitar una vez resuelto.
    private static final Logger DIAG = LoggerFactory.getLogger("DIAG_SAGA");

    private final InboxIdempotente inbox;
    private final SagaDeConfirmacionDeVenta saga;
    private final ObjectMapper json;

    public ConsumidorDeReservas(InboxIdempotente inbox, SagaDeConfirmacionDeVenta saga,
            ObjectMapper json) {
        this.inbox = inbox;
        this.saga = saga;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        DIAG.info("recibir() invocado: evento={}", evento == null ? "null"
                : evento.id() + " tipo=" + evento.tipoEvento());
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!"stock_reservado".equals(tipoEvento) && !"stock_reserva_fallida".equals(tipoEvento)) {
            DIAG.info("recibir() descarta por tipo: {}", tipoEvento);
            return;
        }
        DIAG.info("recibir() procesando tipo={} correlacion(raw)={}", tipoEvento, evento.agregadoId());

        UUID mensajeId = evento.id();
        String cuerpo = serializarPayload(evento.payload());
        Map<String, Object> datos = leer(cuerpo);

        UUID negocioId = evento.negocioId();
        if (negocioId == null && datos.get("negocio_id") != null) {
            negocioId = UUID.fromString(datos.get("negocio_id").toString());
        }
        UUID correlacionId = datos.get("correlacion_id") != null
                ? UUID.fromString(datos.get("correlacion_id").toString())
                : evento.agregadoId();

        final UUID nId = negocioId;
        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("ventas.ConsumidorDeReservas", 
                mensajeId, nId, tipoEvento, cuerpo, payload -> {
                    if ("stock_reserva_fallida".equals(tipoEvento)) {
                        saga.alReservaFallida(correlacionId, motivoDe(datos));
                    } else {
                        saga.alStockReservado(correlacionId);
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
            return "{}";
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de reserva", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static String motivoDe(Map<String, Object> datos) {
        Object faltantes = datos.get("faltantes");
        if (!(faltantes instanceof List<?> lista) || lista.isEmpty()) {
            return "no hay stock suficiente";
        }
        return lista.stream()
                .map(f -> (Map<String, Object>) f)
                .map(f -> "producto " + f.get("producto_id") + ": faltan "
                        + f.get("solicitado") + " y hay " + f.get("disponible"))
                .collect(Collectors.joining("; "));
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("VENTAS"), Set.of(), Set.of());
    }
}
