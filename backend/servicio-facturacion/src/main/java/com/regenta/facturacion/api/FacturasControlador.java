package com.regenta.facturacion.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.facturacion.aplicacion.FacturaDetalle;
import com.regenta.facturacion.aplicacion.FacturaEmitida;
import com.regenta.facturacion.aplicacion.GestionDeFacturas;
import com.regenta.facturacion.aplicacion.ResultadoDeEnvio;
import com.regenta.facturacion.aplicacion.SolicitudDeEnvio;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Consulta de facturas. HU-053. La emisión no tiene endpoint: nace de los
 * eventos de cierre de los tres patrones.
 */
@RestController
@RequestMapping("/api/facturacion/facturas")
@Tag(name = "Facturas", description = "Las facturas emitidas y su detalle")
public class FacturasControlador {

    private final GestionDeFacturas facturas;

    public FacturasControlador(GestionDeFacturas facturas) {
        this.facturas = facturas;
    }

    @GetMapping
    @Operation(summary = "Las facturas del negocio, de la más reciente a la más antigua")
    public List<FacturaEmitida> listar() {
        return facturas.listar();
    }

    @GetMapping("/{facturaId}")
    @Operation(summary = "El detalle de una factura, con el emisor y el cliente como estaban al emitir")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public FacturaDetalle ver(@PathVariable UUID facturaId) {
        return facturas.ver(facturaId);
    }

    @PostMapping("/{facturaId}/envio-cliente")
    @Operation(summary = "Envía la factura aceptada al cliente por correo, con el PDF y el XML")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    @ApiResponse(responseCode = "422", description = "La factura no está aceptada, o no hay correo")
    public ResultadoDeEnvio enviar(@PathVariable UUID facturaId,
            @Valid @RequestBody(required = false) SolicitudDeEnvio solicitud) {
        return facturas.enviarAlCliente(facturaId, solicitud == null ? null : solicitud.correo());
    }
}
