package com.regenta.inventario.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.aplicacion.GestionDeProductos;
import com.regenta.inventario.aplicacion.ProductoDelNegocio;
import com.regenta.inventario.aplicacion.SolicitudDeProducto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Catalogo de productos. HU-028. */
@RestController
@RequestMapping("/api/inventario/productos")
@Tag(name = "Productos", description = "El catalogo, con los atributos variables por categoria")
public class ProductosControlador {

    private final GestionDeProductos productos;

    public ProductosControlador(GestionDeProductos productos) {
        this.productos = productos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un producto y valida sus atributos contra la categoria")
    @ApiResponse(responseCode = "409", description = "SKU o codigo de barras repetido")
    @ApiResponse(responseCode = "422", description = "Faltan atributos obligatorios o son del tipo equivocado")
    public ProductoDelNegocio crear(@Valid @RequestBody SolicitudDeProducto solicitud) {
        return productos.crear(solicitud);
    }

    @GetMapping("/{productoId}")
    @Operation(summary = "Un producto")
    public ProductoDelNegocio ver(@PathVariable UUID productoId) {
        return productos.ver(productoId);
    }
}
