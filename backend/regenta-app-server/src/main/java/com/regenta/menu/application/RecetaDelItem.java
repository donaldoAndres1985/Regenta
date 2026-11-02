package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.regenta.menu.domain.Receta;

/** La receta de un Ã­tem (HU-079): sus lÃ­neas y el costo estimado que sale de ellas. */
public record RecetaDelItem(
        UUID itemMenuId,
        BigDecimal costoEstimado,
        List<LineaDeReceta> lineas) {

    public record LineaDeReceta(
            UUID id,
            UUID productoId,
            String nombreSnapshot,
            BigDecimal cantidad,
            String unidad,
            BigDecimal mermaPct,
            BigDecimal cantidadConMerma,
            boolean opcional) {

        static LineaDeReceta de(Receta r) {
            return new LineaDeReceta(r.getId(), r.getProductoId(), r.getNombreSnapshot(),
                    r.getCantidad(), r.getUnidad(), r.getMermaPct(), r.cantidadConMerma(),
                    r.isOpcional());
        }
    }
}
