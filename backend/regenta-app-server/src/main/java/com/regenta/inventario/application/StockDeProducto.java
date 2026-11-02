package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** El stock de un producto: linea por bodega y los totales. */
public record StockDeProducto(
        UUID productoId,
        List<ExistenciaEnBodega> porBodega,
        BigDecimal total,
        BigDecimal totalDisponible) {
}
