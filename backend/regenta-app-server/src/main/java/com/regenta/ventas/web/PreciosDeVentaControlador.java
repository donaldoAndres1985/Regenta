package com.regenta.ventas.web;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.application.PreciosDelCarrito;
import com.regenta.ventas.application.PreciosDelCarrito.PrecioDelProducto;
import com.regenta.ventas.application.PreciosDelCarrito.SolicitudDePrecios;
import com.regenta.ventas.application.PreciosDelCarrito.SolicitudDePrevision;
import com.regenta.ventas.application.RecalculoDePreciosPorCliente.PrevisionDePrecios;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** HU-132 · Los precios del carrito del POS según la lista de precios del cliente. */
@RestController
@RequestMapping("/api/ventas/precios")
@Tag(name = "Ventas", description = "Precios por lista del cliente")
public class PreciosDeVentaControlador {

    private final PreciosDelCarrito carrito;

    public PreciosDeVentaControlador(PreciosDelCarrito carrito) {
        this.carrito = carrito;
    }

    @PostMapping("/prevision")
    @Operation(summary = "Qué precios del carrito cambiarían al asignar ese cliente (HU-132)",
            description = "Con la lista del cliente (sin lista propia: los precios del negocio). Las líneas con "
                    + "descuento a mano o precio especial (fracción, cotización) no cambian. No guarda nada.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso VENTAS_VENTA_CREAR")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    public PrevisionDePrecios prever(@Valid @RequestBody SolicitudDePrevision solicitud) {
        return carrito.prever(solicitud);
    }

    @PostMapping
    @Operation(summary = "El precio de unos productos con la lista del carrito (HU-132)",
            description = "Con su tramo por volumen; sin lista, o si el producto no está en ella, el del catálogo.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso VENTAS_VENTA_CREAR")
    @ApiResponse(responseCode = "404", description = "Un producto no está en el catálogo")
    public List<PrecioDelProducto> precios(@Valid @RequestBody SolicitudDePrecios solicitud) {
        return carrito.precios(solicitud);
    }
}
