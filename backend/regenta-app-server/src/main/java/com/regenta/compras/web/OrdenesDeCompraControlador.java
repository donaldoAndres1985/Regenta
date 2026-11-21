package com.regenta.compras.web;

import com.regenta.compras.api.*;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.FinDeOrdenesDeCompra;
import com.regenta.compras.application.GestionDeOrdenesDeCompra;
import com.regenta.compras.application.OrdenDelNegocio;
import com.regenta.compras.application.SolicitudDeFinDeOrden;
import com.regenta.compras.application.SolicitudDeOrden;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ã“rdenes de compra con aprobaciÃ³n. HU-047. */
@RestController
@RequestMapping("/api/compras/ordenes")
@Tag(name = "Ã“rdenes de compra", description = "Lo que se le pide a un proveedor, aprobado antes de enviarlo")
public class OrdenesDeCompraControlador {

    private final GestionDeOrdenesDeCompra ordenes;
    private final FinDeOrdenesDeCompra fin;

    public OrdenesDeCompraControlador(GestionDeOrdenesDeCompra ordenes, FinDeOrdenesDeCompra fin) {
        this.ordenes = ordenes;
        this.fin = fin;
    }

    @GetMapping
    @Operation(summary = "Las Ã³rdenes de compra del negocio, la mÃ¡s reciente primero")
    public List<OrdenDelNegocio> listar() {
        return ordenes.listar();
    }

    @GetMapping("/{ordenId}")
    @Operation(summary = "Una orden con sus lÃ­neas y el avance de recepciÃ³n por lÃ­nea")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public OrdenDelNegocio ver(@PathVariable UUID ordenId) {
        return ordenes.ver(ordenId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una orden en borrador con sus lÃ­neas")
    public OrdenDelNegocio crear(@Valid @RequestBody SolicitudDeOrden solicitud) {
        return ordenes.crear(solicitud);
    }

    @PutMapping("/{ordenId}/lineas")
    @Operation(summary = "Reemplaza las lÃ­neas de una orden en borrador")
    @ApiResponse(responseCode = "409", description = "La orden ya no estÃ¡ en borrador")
    public OrdenDelNegocio editarLineas(@PathVariable UUID ordenId,
            @Valid @RequestBody List<SolicitudDeOrden.LineaDeSolicitud> lineas) {
        return ordenes.editarLineas(ordenId, lineas);
    }

    @PostMapping("/{ordenId}/confirmacion")
    @Operation(summary = "Confirma la orden (HU-173): se aprueba sola si el negocio no exige aprobación "
            + "para ese monto; si la exige, queda esperando aprobación")
    @ApiResponse(responseCode = "409", description = "La orden no está en borrador o no tiene líneas")
    public OrdenDelNegocio confirmar(@PathVariable UUID ordenId) {
        return ordenes.confirmar(ordenId);
    }

    @PostMapping("/{ordenId}/aprobacion")
    @Operation(summary = "Aprueba la orden: queda quiÃ©n aprobÃ³ y cuÃ¡ndo")
    @ApiResponse(responseCode = "403", description = "Falta COMPRAS_COMPRA_APROBAR")
    @ApiResponse(responseCode = "409", description = "La orden no estÃ¡ en borrador")
    public OrdenDelNegocio aprobar(@PathVariable UUID ordenId) {
        return ordenes.aprobar(ordenId);
    }

    @GetMapping(value = "/{ordenId}/pdf", produces = "application/pdf")
    @Operation(summary = "El PDF de la orden para mandárselo al proveedor (HU-174)")
    @ApiResponse(responseCode = "409", description = "La orden no está aprobada")
    public org.springframework.http.ResponseEntity<byte[]> pdf(@PathVariable UUID ordenId) {
        byte[] pdf = ordenes.pdf(ordenId);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"orden-de-compra.pdf\"")
                .body(pdf);
    }

    @PostMapping("/{ordenId}/envio")
    @Operation(summary = "Marca la orden como enviada al proveedor")
    @ApiResponse(responseCode = "409", description = "La orden no estÃ¡ aprobada")
    public OrdenDelNegocio enviar(@PathVariable UUID ordenId) {
        return ordenes.enviar(ordenId);
    }

    @PostMapping("/{ordenId}/anulacion")
    @Operation(summary = "Anula una orden que no va (HU-176)",
            description = "Con motivo; queda quién y cuándo. Solo si no ha recibido mercancía.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_COMPRA_ANULAR")
    @ApiResponse(responseCode = "409", description = "Ya recibió mercancía (se cierra con faltante) "
            + "o ya está anulada, cerrada o recibida")
    @ApiResponse(responseCode = "422", description = "Falta el motivo")
    public OrdenDelNegocio anular(@PathVariable UUID ordenId,
            @Valid @RequestBody SolicitudDeFinDeOrden solicitud) {
        return fin.anular(ordenId, solicitud);
    }

    @PostMapping("/{ordenId}/cierre")
    @Operation(summary = "Cierra con faltante una orden recibida en parte (HU-176)",
            description = "Con motivo; no admite más recepciones y lo que faltó sigue en sus líneas.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_COMPRA_ANULAR")
    @ApiResponse(responseCode = "409", description = "La orden no está PARCIAL")
    @ApiResponse(responseCode = "422", description = "Falta el motivo")
    public OrdenDelNegocio cerrar(@PathVariable UUID ordenId,
            @Valid @RequestBody SolicitudDeFinDeOrden solicitud) {
        return fin.cerrar(ordenId, solicitud);
    }
}
