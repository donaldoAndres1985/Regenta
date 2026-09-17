package com.regenta.comandas.aplicacion;

import jakarta.validation.constraints.Size;

/** Crea una cuenta vacía sobre la que se van marcando líneas (HU-089). */
public record SolicitudDeCuenta(@Size(max = 40) String etiqueta, String modoDivision) {
}
