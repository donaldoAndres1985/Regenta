package com.regenta.inventario.web;

import com.regenta.inventario.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.application.GestionDeListasDePrecios;
import com.regenta.inventario.application.ListaDelNegocio;
import com.regenta.inventario.application.PrecioResuelto;
import com.regenta.inventario.application.SolicitudDeLista;
import com.regenta.inventario.application.SolicitudDePrecio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Listas de precios y precios por volumen. HU-036. */
@RestController
@RequestMapping("/api/inventario/listas-precios")
@Tag(name = "Listas de precios", description = "Precios por lista y por volumen sin duplicar el catÃ¡logo")
public class ListasDePreciosControlador {

    private final GestionDeListasDePrecios listas;

    public ListasDePreciosControlador(GestionDeListasDePrecios listas) {
        this.listas = listas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una lista de precios")
    @ApiResponse(responseCode = "409", description = "Ya hay una lista con ese nombre")
    public ListaDelNegocio crear(@Valid @RequestBody SolicitudDeLista solicitud) {
        return listas.crearLista(solicitud);
    }

    @GetMapping
    @Operation(summary = "Las listas de precios vigentes del negocio")
    public List<ListaDelNegocio> vigentes() {
        return listas.listasVigentes();
    }

    @PostMapping("/precios")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Fija el precio de un producto en una lista, desde cierta cantidad")
    public void fijarPrecio(@Valid @RequestBody SolicitudDePrecio solicitud) {
        listas.fijarPrecio(solicitud);
    }

    @GetMapping("/resolver")
    @Operation(summary = "El precio que aplica para un producto en una lista, dada la cantidad")
    public PrecioResuelto resolver(@RequestParam UUID listaId, @RequestParam UUID productoId,
            @RequestParam BigDecimal cantidad) {
        return listas.resolverPrecio(listaId, productoId, cantidad);
    }

    @GetMapping("/validar-descuento")
    @Operation(summary = "Comprueba que un descuento no supere el mÃ¡ximo de la lista")
    @ApiResponse(responseCode = "422", description = "El descuento supera el mÃ¡ximo de la lista")
    public PrecioResuelto validarDescuento(@RequestParam UUID listaId, @RequestParam UUID productoId,
            @RequestParam BigDecimal cantidad, @RequestParam BigDecimal descuentoPct) {
        return listas.validarDescuento(listaId, productoId, cantidad, descuentoPct);
    }
}
