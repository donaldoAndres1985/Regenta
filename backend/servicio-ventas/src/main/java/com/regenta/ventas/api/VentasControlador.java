package com.regenta.ventas.api;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.aplicacion.GestionDeVentas;
import com.regenta.ventas.aplicacion.LineaDeVenta;
import com.regenta.ventas.aplicacion.SolicitudDeLinea;
import com.regenta.ventas.aplicacion.SolicitudDeVenta;
import com.regenta.ventas.aplicacion.VentaDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ventas del patrón Venta directa. HU-037. */
@RestController
@RequestMapping("/api/ventas")
@Tag(name = "Ventas", description = "Armar y confirmar una venta")
public class VentasControlador {

    private final GestionDeVentas ventas;

    public VentasControlador(GestionDeVentas ventas) {
        this.ventas = ventas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una venta en borrador con su número consecutivo")
    public VentaDelNegocio crear(@Valid @RequestBody SolicitudDeVenta solicitud) {
        return ventas.crearBorrador(solicitud);
    }

    @PostMapping("/{ventaId}/lineas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una línea con el snapshot del producto")
    @ApiResponse(responseCode = "409", description = "La venta ya no está en borrador")
    public LineaDeVenta agregarLinea(@PathVariable UUID ventaId,
            @Valid @RequestBody SolicitudDeLinea solicitud) {
        return ventas.agregarLinea(ventaId, solicitud);
    }

    @PatchMapping("/{ventaId}/lineas/{linea}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cambia la cantidad de una línea y recalcula los totales")
    @ApiResponse(responseCode = "409", description = "La venta ya no está en borrador")
    public void cambiarCantidad(@PathVariable UUID ventaId, @PathVariable short linea,
            @RequestBody CantidadNueva cuerpo) {
        ventas.cambiarCantidad(ventaId, linea, cuerpo.cantidad());
    }

    @PostMapping("/{ventaId}/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirma la venta (la saga con Inventario llega en HU-038)")
    @ApiResponse(responseCode = "409", description = "La venta ya no está en borrador")
    public void confirmar(@PathVariable UUID ventaId) {
        ventas.confirmar(ventaId);
    }

    @GetMapping("/{ventaId}")
    @Operation(summary = "Una venta con sus líneas y sus totales guardados")
    public VentaDelNegocio ver(@PathVariable UUID ventaId) {
        return ventas.ver(ventaId);
    }

    /** Cuerpo del PATCH de cantidad. */
    public record CantidadNueva(BigDecimal cantidad) {
    }
}
