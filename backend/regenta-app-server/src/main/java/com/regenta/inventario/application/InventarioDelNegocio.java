package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.List;

/** HU-153 · una página del inventario general y los totales de todo lo filtrado. */
public record InventarioDelNegocio(
        List<FilaDeInventario> filas,
        int totalFilas,
        BigDecimal valorCosto,
        BigDecimal valorPrecio) {
}
