package com.regenta.reportes.infrastructure;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
import com.regenta.reportes.application.ResolverDeDimensiones;

/**
 * Puebla {@code hechos_reserva} con las reservas que no llegaron a estancia
 * (HU-135). {@code penalizacion} se quedaba siempre en cero porque nadie
 * escuchaba {@code reserva_cancelada} ni {@code reserva_no_show}, aunque los
 * dos ya publican la penalización real (HU-071).
 *
 * <p>A propósito no llama a {@code AgregadorDeOcupacion}: una reserva
 * cancelada o un no-show nunca llegan a {@code estancia_finalizada}, así que
 * esas noches nunca entraron a {@code ocupacion_diaria} (criterio 4).
 */
@Component
public class ConsumidorDeReservasCanceladas {

    private final InboxIdempotente inbox;
    private final ResolverDeDimensiones dimensiones;
    private final EscritorDeHechos hechos;
    private final ObjectMapper json;

    public ConsumidorDeReservasCanceladas(InboxIdempotente inbox, ResolverDeDimensiones dimensiones,
            EscritorDeHechos hechos, ObjectMapper json) {
        this.inbox = inbox;
        this.dimensiones = dimensiones;
        this.hechos = hechos;
        this.json = json;
    }

        private static final Set<String> EVENTOS_SOPORTADOS = Set.of("reserva_cancelada", "reserva_no_show");

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

        String estadoFinal = "reserva_no_show".equals(tipoEvento) ? "NO_SHOW" : "CANCELADA"; ContextoDeNegocio.en(contextoDe(nId), () -> inbox.procesarUnaVez(mensajeId, nId, tipoEvento, cuerpo, payload -> procesar(nId, estadoFinal, datos)));
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

    private void procesar(UUID negocioId, String estadoFinal, Map<String, Object> datos) {
        UUID reservaId = uuid(datos.get("reserva_id"));
        UUID clienteId = uuid(datos.get("cliente_id"));
        UUID recursoId = uuid(datos.get("recurso_id"));
        UUID tipoRecursoId = uuid(datos.get("tipo_recurso_id"));
        int noches = datos.get("noches") == null ? 0 : Integer.parseInt(datos.get("noches").toString());
        BigDecimal penalizacion = numero(datos.get("penalizacion"));
        OffsetDateTime ocurridoEn = OffsetDateTime.now();

        int fechaId = dimensiones.fechaId(ocurridoEn);
        Long clienteSk = dimensiones.clienteSk(negocioId, clienteId, null);

        // Sin ocupación que registrar (criterio 4) y sin ingreso reconocido: la
        // habitación no se vendió. Lo único que hubo es la penalización.
        hechos.insertarReserva(negocioId, fechaId, ocurridoEn, null, clienteSk, reservaId, tipoRecursoId,
                recursoId, estadoFinal, noches, BigDecimal.ZERO, BigDecimal.ZERO, penalizacion, null);
    }

    private static BigDecimal numero(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
                        Map<String, Object> mapa = json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
            return mapa;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento de cancelación", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "RESERVA", Set.of(), Set.of("REPORTES"), Set.of(),
                Set.of());
    }
}
