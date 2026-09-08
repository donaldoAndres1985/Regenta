package com.regenta.compras.aplicacion;

import java.math.BigDecimal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de un proveedor (HU-046). */
public record SolicitudDeProveedor(
        @Size(max = 10) String tipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @NotBlank @Size(max = 200) String razonSocial,
        @Size(max = 150) String nombreComercial,
        @Size(max = 120) String contactoNombre,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String telefono,
        @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @PositiveOrZero Integer diasCredito,
        @PositiveOrZero BigDecimal cupoCredito,
        @Min(1) @Max(5) Integer calificacion,
        @Size(max = 4000) String notas) {
}
