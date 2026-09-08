package com.regenta.recursos.aplicacion;

import java.util.List;

/**
 * El resultado de crear un bloqueo (HU-067). El bloqueo queda creado siempre;
 * {@code reservasAfectadas} trae las reservas confirmadas que caen dentro del
 * periodo para que el administrador las gestione (criterio 3). Lista vacía = sin
 * conflictos.
 */
public record BloqueoCreado(
        BloqueoDelNegocio bloqueo,
        List<ReservaAfectada> reservasAfectadas) {

    public boolean hayReservasAfectadas() {
        return !reservasAfectadas.isEmpty();
    }
}
