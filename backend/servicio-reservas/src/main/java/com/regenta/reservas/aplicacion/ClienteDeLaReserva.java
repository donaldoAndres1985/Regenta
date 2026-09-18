package com.regenta.reservas.aplicacion;

import java.util.UUID;

/**
 * Lo que Reservas necesita saber del huésped al cerrar la estancia (HU-118):
 * a nombre de quién sale la factura. El resto de la ficha es de
 * servicio-clientes y aquí no hace falta.
 */
public record ClienteDeLaReserva(
        UUID id,
        String nombre,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion) {
}
