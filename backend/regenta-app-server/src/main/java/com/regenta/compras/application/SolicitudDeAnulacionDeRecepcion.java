package com.regenta.compras.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-177 · Por qué se anula una recepción. */
public record SolicitudDeAnulacionDeRecepcion(@NotBlank @Size(max = 200) String motivo) {
}
