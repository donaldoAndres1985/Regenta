package com.regenta.inventario.application;

import java.util.List;

/**
 * HU-164 · una fila del Excel tal como se leyó (texto crudo de cada columna) y sus errores.
 * {@code numero} es la fila de la hoja (la 1 son los títulos). Sin errores, se importa.
 */
public record FilaImportada(int numero, String sku, String codigoBarras, String nombre, String categoria,
        String unidad, String tarifa, String precio, String costo, String existencia, String bodega,
        List<String> errores) {

    public boolean valida() {
        return errores.isEmpty();
    }
}
