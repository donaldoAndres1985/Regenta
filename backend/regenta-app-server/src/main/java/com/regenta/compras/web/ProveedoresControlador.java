package com.regenta.compras.web;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.GestionDeProveedores;
import com.regenta.compras.application.ProductoDeProveedor;
import com.regenta.compras.application.ProductoParaComprarDelProveedor;
import com.regenta.compras.application.ProveedorDeProducto;
import com.regenta.compras.application.ProveedorDelNegocio;
import com.regenta.compras.application.SolicitudDeProductoDeProveedor;
import com.regenta.compras.application.SolicitudDeProductoNuevo;
import com.regenta.compras.application.SolicitudDeProveedor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Proveedores y qué productos venden. HU-046 y HU-166. */
@RestController
@RequestMapping("/api/compras/proveedores")
@Tag(name = "Proveedores", description = "A quién comprarle, con qué plazo y a qué costo")
public class ProveedoresControlador {

    private final GestionDeProveedores proveedores;

    public ProveedoresControlador(GestionDeProveedores proveedores) {
        this.proveedores = proveedores;
    }

    @GetMapping
    @Operation(summary = "Los proveedores del negocio, con su deuda calculada",
            description = "Por omisión solo los activos; `estado=TODOS` o `INACTIVOS` para el resto.")
    public List<ProveedorDelNegocio> listar(
            @RequestParam(defaultValue = "ACTIVOS") GestionDeProveedores.Estado estado) {
        return proveedores.listar(estado);
    }

    @GetMapping("/{proveedorId}")
    @Operation(summary = "Un proveedor por id, activo o no")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ProveedorDelNegocio ver(@PathVariable UUID proveedorId) {
        return proveedores.ver(proveedorId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un proveedor con sus condiciones de crédito")
    @ApiResponse(responseCode = "409", description = "Ya existe un proveedor con ese documento")
    public ProveedorDelNegocio crear(@Valid @RequestBody SolicitudDeProveedor solicitud) {
        return proveedores.crear(solicitud);
    }

    @PutMapping("/{proveedorId}")
    @Operation(summary = "Cambia los datos y las condiciones de un proveedor")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    @ApiResponse(responseCode = "409", description = "El proveedor está desactivado")
    public ProveedorDelNegocio actualizar(@PathVariable UUID proveedorId,
            @Valid @RequestBody SolicitudDeProveedor solicitud) {
        return proveedores.actualizar(proveedorId, solicitud);
    }

    @DeleteMapping("/{proveedorId}")
    @Operation(summary = "Desactiva un proveedor (no se borra: queda inactivo)")
    public ProveedorDelNegocio desactivar(@PathVariable UUID proveedorId) {
        return proveedores.desactivar(proveedorId);
    }

    @PostMapping("/{proveedorId}/reactivacion")
    @Operation(summary = "Reactiva un proveedor desactivado")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ProveedorDelNegocio reactivar(@PathVariable UUID proveedorId) {
        return proveedores.reactivar(proveedorId);
    }

    @GetMapping("/{proveedorId}/productos")
    @Operation(summary = "Los productos que vende el proveedor, con su código y costo")
    public List<ProductoDeProveedor> productos(@PathVariable UUID proveedorId) {
        return proveedores.productosDe(proveedorId);
    }

    @PostMapping("/{proveedorId}/productos")
    @Operation(summary = "Asocia (o actualiza) un producto del proveedor con su costo",
            description = "Si se marca preferido, deja de serlo el proveedor que lo era.")
    @ApiResponse(responseCode = "404", description = "El producto no existe en el catálogo")
    @ApiResponse(responseCode = "409", description = "El proveedor está desactivado")
    public ProductoDeProveedor asociarProducto(@PathVariable UUID proveedorId,
            @Valid @RequestBody SolicitudDeProductoDeProveedor solicitud) {
        return proveedores.asociarProducto(proveedorId, solicitud);
    }

    @DeleteMapping("/{proveedorId}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Saca un producto del catálogo del proveedor")
    public void quitarProducto(@PathVariable UUID proveedorId, @PathVariable UUID productoId) {
        proveedores.quitarProducto(proveedorId, productoId);
    }

    @GetMapping("/{proveedorId}/para-comprar/{productoId}")
    @Operation(summary = "Tarifa de IVA, regla de lote y costo sugerido para comprarle un producto")
    @ApiResponse(responseCode = "404", description = "El proveedor o el producto no existen")
    public ProductoParaComprarDelProveedor paraComprar(@PathVariable UUID proveedorId,
            @PathVariable UUID productoId) {
        return proveedores.paraComprar(proveedorId, productoId);
    }

    @GetMapping("/de-producto/{productoId}")
    @Operation(summary = "Los proveedores de un producto, con el preferido primero")
    public List<ProveedorDeProducto> deProducto(@PathVariable UUID productoId) {
        return proveedores.proveedoresDe(productoId);
    }

    @PostMapping("/{proveedorId}/productos-nuevos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un producto que llega por primera vez, sin salir de la recepción (HU-180)",
            description = "Lo da de alta en Inventario con sus reglas de siempre y lo deja en el catálogo del "
                    + "proveedor con su código y su costo. Devuelve lo necesario para agregar la línea.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso INVENTARIO_PRODUCTO_CREAR")
    @ApiResponse(responseCode = "409", description = "Ya hay un producto con ese SKU o código de barras")
    @ApiResponse(responseCode = "422", description = "Falta el SKU, el nombre, la categoría, la unidad o la "
            + "tarifa de IVA, con los mismos mensajes de Inventario")
    public ProductoParaComprarDelProveedor crearProductoNuevo(@PathVariable UUID proveedorId,
            @Valid @RequestBody SolicitudDeProductoNuevo solicitud) {
        return proveedores.crearProductoNuevo(proveedorId, solicitud);
    }
}
