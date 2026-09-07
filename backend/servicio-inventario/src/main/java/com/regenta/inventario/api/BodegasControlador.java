package com.regenta.inventario.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.aplicacion.BodegaDelNegocio;
import com.regenta.inventario.aplicacion.ConsultaDeExistencias;
import com.regenta.inventario.aplicacion.GestionDeBodegas;
import com.regenta.inventario.aplicacion.SolicitudDeBodega;
import com.regenta.inventario.aplicacion.StockDeProducto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Bodegas y existencias. HU-029. */
@RestController
@RequestMapping("/api/inventario")
@Tag(name = "Bodegas y existencias", description = "Donde esta el stock y cuanto hay")
public class BodegasControlador {

    private final GestionDeBodegas bodegas;
    private final ConsultaDeExistencias existencias;

    public BodegasControlador(GestionDeBodegas bodegas, ConsultaDeExistencias existencias) {
        this.bodegas = bodegas;
        this.existencias = existencias;
    }

    @GetMapping("/bodegas")
    @Operation(summary = "Las bodegas del negocio")
    public List<BodegaDelNegocio> listar() {
        return bodegas.listar();
    }

    @PostMapping("/bodegas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una bodega")
    @ApiResponse(responseCode = "409", description = "Ya hay una bodega con ese codigo")
    public BodegaDelNegocio crear(@Valid @RequestBody SolicitudDeBodega solicitud) {
        return bodegas.crear(solicitud);
    }

    @DeleteMapping("/bodegas/{bodegaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una bodega vacia")
    @ApiResponse(responseCode = "409", description = "La bodega tiene existencias")
    public void eliminar(@PathVariable UUID bodegaId) {
        bodegas.eliminar(bodegaId);
    }

    @GetMapping("/productos/{productoId}/stock")
    @Operation(summary = "El stock de un producto por bodega, con el total")
    public StockDeProducto stock(@PathVariable UUID productoId) {
        return existencias.delProducto(productoId);
    }
}
