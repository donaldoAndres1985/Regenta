package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * La sugerencia FEFO para una salida: que lotes tomar, empezando por el que
 * vence antes. {@code suficiente} dice si los lotes vigentes alcanzan.
 */
public record SugerenciaFefo(
        UUID productoId,
        UUID bodegaId,
        BigDecimal cantidadSolicitada,
        boolean suficiente,
        List<AsignacionFefo> asignaciones) {
}
