package com.regenta.clientes.aplicacion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de una interaccion. {@code tipo} es obligatorio; {@code ocurridoEn} por
 * defecto es ahora; {@code seguimientoEn} es opcional y, si viene, dispara el
 * recordatorio al responsable cuando llega esa fecha.
 */
public record SolicitudDeInteraccion(
        @NotBlank @Size(max = 20) String tipo,
        @Size(max = 150) String asunto,
        @Size(max = 8000) String detalle,
        OffsetDateTime ocurridoEn,
        LocalDate seguimientoEn) {
}
