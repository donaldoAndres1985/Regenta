package com.regenta.usuarios.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un impuesto del negocio. Codigos DIAN: 01 IVA, 04 INC. */
public record SolicitudDeImpuesto(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 60) String nombre,
        @NotBlank @Size(max = 20) String tipo,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal porcentaje,
        @Size(max = 20) String aplicaSobre) {
}
