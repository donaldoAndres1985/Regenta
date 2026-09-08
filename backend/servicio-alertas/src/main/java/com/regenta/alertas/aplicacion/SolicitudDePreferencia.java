package com.regenta.alertas.aplicacion;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Fija cómo quiere el usuario un tipo de alerta (HU-094 criterio 4). */
public record SolicitudDePreferencia(
        @NotBlank @Size(max = 40) String tipoCodigo,
        List<String> canales,
        Boolean habilitada,
        LocalTime noMolestarDesde,
        LocalTime noMolestarHasta) {
}
