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
import com.regenta.reportes.application.ZonaHorariaDeNegocios;

/**
 * Cachea la zona horaria del negocio (HU-097 criterio 3). Servicio-reportes no
 * consulta la base de servicio-usuarios —el dato maestro—, así que la toma de
 * este mismo evento con el que servicio-usuarios anuncia el alta (HU-011).
 *
 * <p>Payload esperado de {@code negocio_creado}: {@code {negocio_id,
 * nombre_comercial, plan, patron, estado, pais, moneda, modulos,
 * administrador_id, zona_horaria}}.
 */
@Component
public class ConsumidorDeNegocioCreado {

    private final InboxIdempotente inbox;
    private final ZonaHorariaDeNegocios zonas;
    private final ObjectMapper json;

    public ConsumidorDeNegocioCreado(InboxIdempotente inbox, ZonaHorariaDeNegocios zonas, ObjectMapper json) {
        this.inbox = inbox;
        this.zonas = zonas;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("negocio_creado");

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

        ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(mensajeId, nId, tipoEvento, cuerpo, payload -> procesar(nId, datos)));
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
        Object zona = datos.get("zona_horaria");
        zonas.guardar(negocioId, zona == null ? null : zona.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento negocio_creado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(), Set.of("REPORTES"),
                Set.of(), Set.of());
    }
}
