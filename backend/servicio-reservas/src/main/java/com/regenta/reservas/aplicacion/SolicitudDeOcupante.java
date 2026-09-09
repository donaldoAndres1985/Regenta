package com.regenta.reservas.aplicacion;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Un ocupante de la estancia (HU-072). El titular tiene que traer su documento. */
public record SolicitudDeOcupante(
        boolean esTitular,
        @NotBlank @Size(max = 120) String nombres,
        @Size(max = 120) String apellidos,
        @Size(max = 10) String tipoDocumento,
        @Size(max = 30) String numeroDocumento,
        @Size(max = 2) String nacionalidad,
        LocalDate fechaNacimiento,
        @Size(max = 30) String telefono,
        @Size(max = 150) String email) {
}
