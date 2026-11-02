package com.regenta.comandas.application;

import jakarta.validation.constraints.Size;

/** Crea una cuenta vacía sobre la que se van marcando líneas (HU-089). */
public record SolicitudDeCuenta(@Size(max = 40) String etiqueta, String modoDivision) {
}
