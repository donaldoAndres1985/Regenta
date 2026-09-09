package com.regenta.reservas.aplicacion;

import jakarta.validation.constraints.Size;

/** El motivo por el que se cancela una reserva (HU-071). */
public record SolicitudDeCancelacion(@Size(max = 4000) String motivo) {
}
