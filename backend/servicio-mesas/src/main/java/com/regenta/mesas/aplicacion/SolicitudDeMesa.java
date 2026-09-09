package com.regenta.mesas.aplicacion;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de una mesa (HU-081). El {@code codigo} solo se usa al crear;
 * en la edición se ignora (es inmutable, {@code uq_mesa_codigo}).
 */
public record SolicitudDeMesa(
        UUID zonaId,
        @NotBlank @Size(max = 20) String codigo,
        @Size(max = 60) String nombre,
        @Positive Integer capacidad,
        String forma,
        Integer posX,
        Integer posY,
        Integer ancho,
        Integer alto) {
}
