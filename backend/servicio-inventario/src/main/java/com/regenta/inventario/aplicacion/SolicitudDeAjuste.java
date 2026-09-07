package com.regenta.inventario.aplicacion;

import java.util.List;
import java.util.UUID;

import com.regenta.inventario.domain.TipoAjuste;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de un ajuste en BORRADOR. El {@code motivo} es opcional al cargar pero
 * obligatorio al aplicar (criterio 3 de HU-033).
 */
public record SolicitudDeAjuste(
        @NotBlank @Size(max = 30) String numero,
        @NotNull UUID bodegaId,
        @NotNull TipoAjuste tipo,
        @Size(max = 2000) String motivo,
        @NotEmpty @Valid List<LineaDeAjuste> lineas) {
}
