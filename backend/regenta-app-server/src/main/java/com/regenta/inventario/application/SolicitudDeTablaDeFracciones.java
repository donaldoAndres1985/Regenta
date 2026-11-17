package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** HU-156 · alta o edición de una tabla: nombre y sus fracciones (precio con IVA incluido). */
public record SolicitudDeTablaDeFracciones(
        @NotBlank @Size(max = 100) String nombre,
        @NotEmpty @Valid List<Linea> fracciones) {

    public record Linea(@NotBlank @Size(max = 30) String medida, BigDecimal factor, BigDecimal precio) {
    }
}
