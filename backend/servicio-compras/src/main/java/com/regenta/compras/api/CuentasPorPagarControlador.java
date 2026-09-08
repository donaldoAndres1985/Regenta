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

import com.regenta.compras.aplicacion.CuentaDelNegocio;
import com.regenta.compras.aplicacion.GestionDeCuentasPorPagar;
import com.regenta.compras.aplicacion.SolicitudDeFacturaDeRecepcion;
import com.regenta.compras.aplicacion.SolicitudDePago;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cuentas por pagar y pagos a proveedores. HU-049. */
@RestController
@RequestMapping("/api/compras/cuentas-por-pagar")
@Tag(name = "Cuentas por pagar", description = "Lo que se le debe a cada proveedor y los pagos")
public class CuentasPorPagarControlador {

    private final GestionDeCuentasPorPagar cuentas;

    public CuentasPorPagarControlador(GestionDeCuentasPorPagar cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping
    @Operation(summary = "Las cuentas por pagar, por antigüedad; las vencidas quedan marcadas")
    public List<CuentaDelNegocio> listar(
            @RequestParam(name = "soloVencidas", defaultValue = "false") boolean soloVencidas) {
        return cuentas.listar(soloVencidas);
    }

    @GetMapping("/{cuentaId}")
    @Operation(summary = "Una cuenta por pagar con sus pagos")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public CuentaDelNegocio ver(@PathVariable UUID cuentaId) {
        return cuentas.ver(cuentaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra la factura del proveedor sobre una recepción confirmada")
    @ApiResponse(responseCode = "409", description = "Factura repetida para el proveedor o recepción ya facturada")
    public CuentaDelNegocio registrarFactura(
            @Valid @RequestBody SolicitudDeFacturaDeRecepcion solicitud) {
        return cuentas.registrarFactura(solicitud);
    }

    @PostMapping("/{cuentaId}/pagos")
    @Operation(summary = "Registra un pago: baja el saldo y mueve el estado")
    @ApiResponse(responseCode = "422", description = "El pago supera el saldo")
    public CuentaDelNegocio registrarPago(@PathVariable UUID cuentaId,
            @Valid @RequestBody SolicitudDePago solicitud) {
        return cuentas.registrarPago(cuentaId, solicitud);
    }
}
