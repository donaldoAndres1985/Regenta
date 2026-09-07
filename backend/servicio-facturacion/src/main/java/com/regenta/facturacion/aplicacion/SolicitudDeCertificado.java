package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un certificado. {@code referenciaKms} es la clave en el gestor de
 * secretos: aquí NO se recibe ni se guarda el archivo .p12 (HU-055 criterio 6).
 */
public record SolicitudDeCertificado(
        @NotBlank @Size(max = 80) String alias,
        @Size(max = 150) String emisor,
        @Size(max = 80) String numeroSerie,
        @NotNull LocalDate vigenteDesde,
        @NotNull LocalDate vigenteHasta,
        @NotBlank String referenciaKms) {
}
