package com.regenta.compras.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.aplicacion.GestionDeRecepciones;
import com.regenta.compras.aplicacion.RecepcionDelNegocio;
import com.regenta.compras.aplicacion.SolicitudDeConfirmacionDeRecepcion;
import com.regenta.compras.aplicacion.SolicitudDeRecepcion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Recepción de mercancía de una orden de compra, con captura de lotes. HU-048. */
@RestController
@RequestMapping("/api/compras/recepciones")
@Tag(name = "Recepciones", description = "Lo que llegó de verdad de una orden, con su lote y vencimiento")
public class RecepcionesControlador {

    private final GestionDeRecepciones recepciones;

    public RecepcionesControlador(GestionDeRecepciones recepciones) {
        this.recepciones = recepciones;
    }

    @GetMapping
    @Operation(summary = "Las recepciones del negocio, opcionalmente las de una orden")
    public List<RecepcionDelNegocio> listar(@RequestParam(required = false) UUID ordenId) {
        return recepciones.listar(ordenId);
    }

    @GetMapping("/{recepcionId}")
    @Operation(summary = "Una recepción con sus líneas y los lotes capturados")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public RecepcionDelNegocio ver(@PathVariable UUID recepcionId) {
        return recepciones.ver(recepcionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Arma una recepción en borrador con lo que llegó")
    @ApiResponse(responseCode = "422", description = "Falta el lote donde la categoría lo exige")
    public RecepcionDelNegocio crear(@Valid @RequestBody SolicitudDeRecepcion solicitud) {
        return recepciones.crear(solicitud);
    }

    @PostMapping("/{recepcionId}/confirmacion")
    @Operation(summary = "Confirma la recepción: sube inventario, avanza la orden y abre la cuenta por pagar")
    @ApiResponse(responseCode = "409", description = "La recepción ya no está en borrador")
    @ApiResponse(responseCode = "422", description = "La cantidad recibida supera lo pedido + 5%")
    public RecepcionDelNegocio confirmar(@PathVariable UUID recepcionId,
            @RequestBody(required = false) SolicitudDeConfirmacionDeRecepcion solicitud) {
        return recepciones.confirmar(recepcionId, solicitud);
    }
}
