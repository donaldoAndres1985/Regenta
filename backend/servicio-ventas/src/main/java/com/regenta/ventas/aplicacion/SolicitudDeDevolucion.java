package com.regenta.ventas.aplicacion;

import java.util.List;
import java.util.UUID;

import com.regenta.ventas.domain.MotivoDevolucion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Registrar una devolución. {@code reintegraStock} = false para el producto
 * defectuoso; {@code bodegaDestinoId} dice a qué bodega vuelve lo demás.
 */
public record SolicitudDeDevolucion(
        @NotNull MotivoDevolucion motivo,
        @Size(max = 2000) String detalle,
        boolean reintegraStock,
        UUID bodegaDestinoId,
        @NotEmpty @Valid List<LineaDevuelta> lineas) {
}
