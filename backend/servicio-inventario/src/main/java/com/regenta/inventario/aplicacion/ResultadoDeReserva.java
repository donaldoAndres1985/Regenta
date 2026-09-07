package com.regenta.inventario.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * El resultado de solicitar una reserva. Si {@code reservada} es false,
 * {@code faltantes} dice qué producto y cuánto faltó.
 */
public record ResultadoDeReserva(
        boolean reservada,
        UUID correlacionId,
        List<FaltanteDeStock> faltantes,
        List<UUID> reservaIds) {
}
