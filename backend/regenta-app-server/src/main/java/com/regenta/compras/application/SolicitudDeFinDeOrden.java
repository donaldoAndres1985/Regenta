package com.regenta.compras.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-176 · Por qué se anula una orden o se cierra con faltante. */
public record SolicitudDeFinDeOrden(@NotBlank @Size(max = 200) String motivo) {
}
