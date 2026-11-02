package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.util.List;
import java.util.UUID;

/**
 * El resultado de solicitar una reserva. Si {@code reservada} es false,
 * {@code faltantes} dice quÃ© producto y cuÃ¡nto faltÃ³.
 */
public record ResultadoDeReserva(
        boolean reservada,
        UUID correlacionId,
        List<FaltanteDeStock> faltantes,
        List<UUID> reservaIds) {
}
