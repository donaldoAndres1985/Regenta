package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

import java.util.List;

public record ResultadoDeBusqueda(
        List<ProductoEncontrado> productos,
        long total) {
}
