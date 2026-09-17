package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cobra una cuenta (HU-090 criterio 2): el método es obligatorio, la propina no. */
public record SolicitudDePago(
        @NotBlank String metodo,
        BigDecimal montoRecibido,
        BigDecimal propina,
        @Size(max = 80) String referencia) {
}
