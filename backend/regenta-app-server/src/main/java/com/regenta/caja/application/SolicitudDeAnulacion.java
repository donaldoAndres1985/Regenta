package com.regenta.caja.application;

/** HU-141 · anular un movimiento manual de caja: el motivo es obligatorio. */
public record SolicitudDeAnulacion(String motivo) {
}
