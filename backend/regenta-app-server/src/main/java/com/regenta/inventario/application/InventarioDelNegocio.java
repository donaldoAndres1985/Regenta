package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * HU-153 · una página del inventario general y los totales de todo lo filtrado.
 * {@code valorCosto} va nulo sin INVENTARIO_EXISTENCIA_VER; {@code bajoMinimo} cuenta los que
 * están en su mínimo o por debajo dentro de lo filtrado; {@code porRevisar}, los de lo filtrado
 * con la tarifa de IVA sin confirmar.
 */
public record InventarioDelNegocio(
        List<FilaDeInventario> filas,
        int totalFilas,
        BigDecimal valorCosto,
        BigDecimal valorPrecio,
        int bajoMinimo,
        int porRevisar) {
}
