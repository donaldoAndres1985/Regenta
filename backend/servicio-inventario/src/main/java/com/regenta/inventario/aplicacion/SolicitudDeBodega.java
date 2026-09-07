package com.regenta.inventario.aplicacion;

import java.util.UUID;

import com.regenta.inventario.domain.TipoBodega;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudDeBodega(
        @NotBlank @Size(max = 20) String codigo,
        @NotBlank @Size(max = 100) String nombre,
        TipoBodega tipo,
        UUID sucursalId) {
}
