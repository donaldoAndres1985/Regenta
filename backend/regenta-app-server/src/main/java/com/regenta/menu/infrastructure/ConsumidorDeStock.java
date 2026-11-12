package com.regenta.menu.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.menu.application.GestionDeDisponibilidad;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * Cuando un movimiento de inventario deja un insumo en cero, marca agotados los
 * ítems cuya receta lo usa (HU-080 criterio 3). Pasa por el Inbox.
 */
@Component
public class ConsumidorDeStock {

    private final InboxIdempotente inbox;
    private final GestionDeDisponibilidad disponibilidad;
    private final ObjectMapper json;

    public ConsumidorDeStock(InboxIdempotente inbox, GestionDeDisponibilidad disponibilidad,
            ObjectMapper json) {
        this.inbox = inbox;
        this.disponibilidad = disponibilidad;
        this.json = json;
    }

    @ApplicationModuleListener
    public void recibir(EventoGenerico eventoGenerico) {
        if (eventoGenerico == null || !"stock_actualizado".equals(eventoGenerico.tipoEvento())) {
            return;
        }

        UUID mensajeId = eventoGenerico.id();
        String tipoEvento = eventoGenerico.tipoEvento();
        String cuerpo = serializarPayload(eventoGenerico.payload());
        Map<String, Object> datos = leer(cuerpo);

        Object negocio = datos.get("negocio_id");
        Object producto = datos.get("producto_id");
        if (negocio == null || producto == null || !enCero(datos.get("saldo_posterior"))) {
            return;
        }
        UUID negocioId = UUID.fromString(negocio.toString());
        UUID productoId = UUID.fromString(producto.toString());

        ContextoDeNegocio.en(contextoDe(negocioId), () -> inbox.procesarUnaVez("menu.ConsumidorDeStock", 
                mensajeId, negocioId, tipoEvento, cuerpo,
                payload -> disponibilidad.agotarPorInsumoSinStock(negocioId, productoId, LocalDate.now())));
    }

    private static boolean enCero(Object saldo) {
        if (saldo == null) {
            return false;
        }
        try {
            return new BigDecimal(saldo.toString()).compareTo(BigDecimal.ZERO) <= 0;
        } catch (NumberFormatException e) {
            return false;
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
            throw new IllegalArgumentException("No se pudo serializar el payload", e);
        }
    }

    private Map<String, Object> leer(String cuerpo) {
        try {
            return json.readValue(cuerpo, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo leer el evento stock_actualizado", e);
        }
    }

    private static DatosDelNegocio contextoDe(UUID negocioId) {
        return new DatosDelNegocio(negocioId, null, "", "COMANDA", Set.of(), Set.of("MENU"),
                Set.of(), Set.of());
    }
}
