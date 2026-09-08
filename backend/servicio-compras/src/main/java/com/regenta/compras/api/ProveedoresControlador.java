package com.regenta.compras.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.aplicacion.GestionDeProveedores;
import com.regenta.compras.aplicacion.ProductoDeProveedor;
import com.regenta.compras.aplicacion.ProveedorDelNegocio;
import com.regenta.compras.aplicacion.ProveedorDeProducto;
import com.regenta.compras.aplicacion.SolicitudDeProductoDeProveedor;
import com.regenta.compras.aplicacion.SolicitudDeProveedor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Proveedores y qué productos venden. HU-046. */
@RestController
@RequestMapping("/api/compras/proveedores")
@Tag(name = "Proveedores", description = "A quién comprarle, con qué plazo y a qué costo")
public class ProveedoresControlador {

    private final GestionDeProveedores proveedores;

    public ProveedoresControlador(GestionDeProveedores proveedores) {
        this.proveedores = proveedores;
    }

    @GetMapping
    @Operation(summary = "Los proveedores activos del negocio")
    public List<ProveedorDelNegocio> listar() {
        return proveedores.listar();
    }

    @GetMapping("/{proveedorId}")
    @Operation(summary = "Un proveedor por id")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ProveedorDelNegocio ver(@PathVariable UUID proveedorId) {
        return proveedores.ver(proveedorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un proveedor con sus condiciones de crédito")
    @ApiResponse(responseCode = "409", description = "Ya hay un proveedor con ese documento")
    public ProveedorDelNegocio crear(@Valid @RequestBody SolicitudDeProveedor solicitud) {
        return proveedores.crear(solicitud);
    }

    @PutMapping("/{proveedorId}")
    @Operation(summary = "Cambia los datos y las condiciones de un proveedor")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ProveedorDelNegocio actualizar(@PathVariable UUID proveedorId,
            @Valid @RequestBody SolicitudDeProveedor solicitud) {
        return proveedores.actualizar(proveedorId, solicitud);
    }

    @DeleteMapping("/{proveedorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva un proveedor (no se borra: queda inactivo)")
    public void desactivar(@PathVariable UUID proveedorId) {
        proveedores.desactivar(proveedorId);
    }

    @GetMapping("/{proveedorId}/productos")
    @Operation(summary = "Los productos que vende el proveedor, con su código y costo")
    public List<ProductoDeProveedor> productos(@PathVariable UUID proveedorId) {
        return proveedores.productosDe(proveedorId);
    }

    @PostMapping("/{proveedorId}/productos")
    @Operation(summary = "Asocia (o actualiza) un producto del proveedor con su costo")
    public ProductoDeProveedor asociarProducto(@PathVariable UUID proveedorId,
            @Valid @RequestBody SolicitudDeProductoDeProveedor solicitud) {
        return proveedores.asociarProducto(proveedorId, solicitud);
    }

    @GetMapping("/de-producto/{productoId}")
    @Operation(summary = "Los proveedores de un producto, con el preferido primero")
    public List<ProveedorDeProducto> deProducto(@PathVariable UUID productoId) {
        return proveedores.proveedoresDe(productoId);
    }
}
