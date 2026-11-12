package com.regenta.reportes.infrastructure;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.regenta.shared.events.EventoGenerico;
import com.fasterxml.jackson.core.type.TypeReference;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.reportes.application.AgregadorDiario;

/**
 * Ajusta el agregado diario cuando una venta se anula (HU-097 criterio 4). No
 * toca {@code hechos_venta} —esa tabla es el histórico de lo que pasó, no se
 * reescribe— solo revierte lo que {@link AgregadorDiario} había sumado al
 * cerrar la venta, usando lo que quedó registrado en
 * {@code agregados_diarios_documento}.
 *
 * <p>Payload esperado de {@code venta_anulada} (HU-041): {@code {negocio_id,
 * venta_id, numero, motivo, lineas:[...]}}.
 */
@Component
public class ConsumidorDeVentasAnuladas {

    private final InboxIdempotente inbox;
    private final AgregadorDiario agregador;
    private final ObjectMapper json;

    public ConsumidorDeVentasAnuladas(InboxIdempotente inbox, AgregadorDiario agregador, ObjectMapper json) {
        this.inbox = inbox;
        this.agregador = agregador;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("venta_anulada");

    @ApplicationModuleListener
    public void recibir(EventoGenerico evento) {
        if (evento == null) {
            return;
        }
        String tipoEvento = evento.tipoEvento();
        if (!EVENTOS_SOPORTADOS.contains(tipoEvento)) {
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

        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez("reportes.ConsumidorDeVentasAnuladas", mensajeId, nId, tipoEvento, cuerpo, payload -> procesar(nId, datos)));
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

    private void procesar(UUID negocioId, Map<String, Object> datos) {
        UUID ventaId = uuid(datos.get("venta_id"));
        if (ventaId == null) {
            return;
        }
        agregador.revertir(negocioId, "VENTA", ventaId);
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento venta_anulada", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }
}
