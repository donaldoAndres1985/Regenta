package com.regenta.facturacion.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.facturacion.aplicacion.GestionDeFirmaYTransmision;
import com.regenta.facturacion.aplicacion.ResultadoDeTransmision;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Firma y transmisión de una factura a la DIAN. HU-055. */
@RestController
@RequestMapping("/api/facturacion/facturas/{facturaId}")
@Tag(name = "Transmisión", description = "Firmar, transmitir y ver el log de una factura")
public class TransmisionesControlador {

    private final GestionDeFirmaYTransmision firma;

    public TransmisionesControlador(GestionDeFirmaYTransmision firma) {
        this.firma = firma;
    }

    @PostMapping("/transmision")
    @RequierePermiso("FACTURACION_FACTURA_CREAR")
    @Operation(summary = "Firma (si hace falta) y transmite la factura; reintento si ya se había intentado")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    @ApiResponse(responseCode = "422", description = "La factura no está lista para transmitir")
    public ResultadoDeTransmision transmitir(@PathVariable UUID facturaId) {
        return firma.firmarYTransmitir(facturaId);
    }

    @GetMapping("/transmisiones")
    @Operation(summary = "El log de intercambios con la DIAN para esta factura")
    public List<Map<String, Object>> log(@PathVariable UUID facturaId) {
        return firma.transmisionesDe(facturaId);
    }
}
