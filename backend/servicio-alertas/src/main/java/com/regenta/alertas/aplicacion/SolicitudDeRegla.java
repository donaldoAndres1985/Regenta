package com.regenta.alertas.aplicacion;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de una regla de alerta (HU-092 criterio 1): tipo, condición,
 * severidad, canales y destinatarios.
 */
public record SolicitudDeRegla(
        @NotBlank @Size(max = 40) String tipoCodigo,
        @NotBlank @Size(max = 80) String nombre,
        UUID sucursalId,
        Map<String, Object> condicion,
        String severidad,
        List<String> canales,
        List<String> destinatariosRoles,
        List<UUID> destinatariosUsuarios,
        String frecuencia,
        LocalTime horaEnvio,
        @PositiveOrZero Integer silenciarHoras) {
}
