package com.regenta.facturacion.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.facturacion.aplicacion.FacturaDetalle;
import com.regenta.facturacion.aplicacion.FacturaEmitida;
import com.regenta.facturacion.aplicacion.GestionDeFacturas;

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
}
