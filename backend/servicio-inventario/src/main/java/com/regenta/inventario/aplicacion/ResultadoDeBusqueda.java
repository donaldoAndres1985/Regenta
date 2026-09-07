package com.regenta.inventario.aplicacion;

import java.util.List;

public record ResultadoDeBusqueda(
        List<ProductoEncontrado> productos,
        long total) {
}
