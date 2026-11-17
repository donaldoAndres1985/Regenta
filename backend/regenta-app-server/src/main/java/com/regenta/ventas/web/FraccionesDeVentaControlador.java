package com.regenta.ventas.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.api.FraccionDeCatalogo;
import com.regenta.ventas.application.PrecioDeLinea;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-158 · las fracciones que el POS ofrece para un producto. */
@RestController
@RequestMapping("/api/ventas/fracciones")
@Tag(name = "Ventas", description = "Fracciones con que se vende un producto")
public class FraccionesDeVentaControlador {

    private final PrecioDeLinea precios;

    public FraccionesDeVentaControlador(PrecioDeLinea precios) {
        this.precios = precios;
    }

    @GetMapping("/{productoId}")
    @Operation(summary = "Las fracciones del producto con su precio (con IVA incluido); vacio si no tiene")
    public List<FraccionDeCatalogo> deProducto(@PathVariable UUID productoId) {
        return precios.fraccionesDe(productoId);
    }
}
