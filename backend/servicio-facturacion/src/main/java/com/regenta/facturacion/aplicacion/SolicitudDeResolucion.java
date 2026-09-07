package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Alta de una resolución de numeración DIAN (HU-052). */
public record SolicitudDeResolucion(
        UUID sucursalId,
        @NotBlank @Size(max = 20) String tipoDocumento,
        @NotBlank @Size(max = 40) String numeroResolucion,
        @Size(max = 10) String prefijo,
        @Positive long rangoDesde,
        @Positive long rangoHasta,
        @Size(max = 120) String claveTecnica,
        @NotNull LocalDate vigenteDesde,
        @NotNull LocalDate vigenteHasta,
        @Size(max = 20) String ambiente) {
}
