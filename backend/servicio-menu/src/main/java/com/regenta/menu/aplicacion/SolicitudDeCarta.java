package com.regenta.menu.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta o edición de una carta (HU-076). */
public record SolicitudDeCarta(
        @NotBlank @Size(max = 80) String nombre,
        String descripcion,
        LocalTime horaDesde,
        LocalTime horaHasta,
        List<Integer> diasSemana,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean esDefault,
        UUID sucursalId) {
}
