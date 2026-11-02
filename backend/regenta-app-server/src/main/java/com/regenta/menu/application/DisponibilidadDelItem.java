package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.menu.domain.DisponibilidadDiaria;

/**
 * El estado de un Ã­tem para una fecha (HU-080): si estÃ¡ agotado, su cupo del dÃ­a
 * y cuÃ¡nto se ha vendido. Sin fila para esa fecha, el Ã­tem estÃ¡ disponible.
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
