package com.regenta.reservas.aplicacion;

import jakarta.validation.Valid;

/**
 * Confirmar el check-out (HU-074). Si queda saldo pendiente, hay que poner
 * {@code confirmarConSaldo = true} para cerrar igual (criterio 5). {@code pago}
 * es opcional: un cobro final para liquidar antes de cerrar.
 */
public record SolicitudDeCheckOut(
        boolean confirmarConSaldo,
        @Valid SolicitudDePagoDeReserva pago) {
}
