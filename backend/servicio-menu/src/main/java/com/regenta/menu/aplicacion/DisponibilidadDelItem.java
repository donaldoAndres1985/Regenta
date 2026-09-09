package com.regenta.menu.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.menu.domain.DisponibilidadDiaria;

/**
 * El estado de un ítem para una fecha (HU-080): si está agotado, su cupo del día
 * y cuánto se ha vendido. Sin fila para esa fecha, el ítem está disponible.
 */
public record DisponibilidadDelItem(
        UUID itemId,
        LocalDate fecha,
        boolean agotado,
        Integer cupoDiario,
        int vendidas) {

    static DisponibilidadDelItem de(DisponibilidadDiaria d) {
        return new DisponibilidadDelItem(d.getItemId(), d.getFecha(), d.estaAgotado(),
                d.getCantidadDisponible(), d.getCantidadVendida());
    }

    static DisponibilidadDelItem disponible(UUID itemId, LocalDate fecha) {
        return new DisponibilidadDelItem(itemId, fecha, false, null, 0);
    }
}
