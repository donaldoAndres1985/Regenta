package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Alta o edición de un ítem de menú (HU-077). */
public record SolicitudDeItem(
        @NotNull UUID categoriaMenuId,
        UUID estacionId,
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 150) String nombre,
        String descripcion,
        String tipo,
        @NotNull @PositiveOrZero BigDecimal precio,
        UUID impuestoId,
        Boolean precioIncluyeImpuesto,
        @PositiveOrZero Integer tiempoPreparacionMin,
        String curso,
        Map<String, Object> atributos,
        String imagenUrl,
        @PositiveOrZero Integer orden) {
}
