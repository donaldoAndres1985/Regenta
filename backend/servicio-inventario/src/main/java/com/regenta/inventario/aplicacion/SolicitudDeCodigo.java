package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Alta de un código de barras alterno para un producto. */
public record SolicitudDeCodigo(
        @NotBlank @Size(max = 60) String codigo,
        @NotNull @Positive BigDecimal factor,
        @Size(max = 60) String descripcion) {
}
