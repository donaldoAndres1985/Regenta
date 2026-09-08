package com.regenta.facturacion.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.facturacion.aplicacion.FacturaEmitida;
import com.regenta.facturacion.aplicacion.GestionDeNotasCredito;
import com.regenta.facturacion.aplicacion.SolicitudDeNotaCredito;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Notas crédito. HU-056. Una factura emitida no se edita: se corrige con una NC. */
@RestController
@RequestMapping("/api/facturacion/facturas/{facturaOrigenId}/notas-credito")
@Tag(name = "Notas crédito", description = "Anular o corregir una factura aceptada con una NC")
public class NotasCreditoControlador {

    private final GestionDeNotasCredito notas;

    public NotasCreditoControlador(GestionDeNotasCredito notas) {
        this.notas = notas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Emite una nota crédito contra la factura de origen, con su código de motivo DIAN")
    @ApiResponse(responseCode = "404", description = "La factura de origen no existe en este negocio")
    @ApiResponse(responseCode = "422", description = "La factura de origen no está aceptada")
    public FacturaEmitida emitir(@PathVariable UUID facturaOrigenId,
            @Valid @RequestBody SolicitudDeNotaCredito solicitud) {
        return notas.emitir(facturaOrigenId, solicitud);
    }
}
