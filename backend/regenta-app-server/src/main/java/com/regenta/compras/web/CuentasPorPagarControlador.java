package com.regenta.compras.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.CuentaDelNegocio;
import com.regenta.compras.application.GestionDeCuentasPorPagar;
import com.regenta.compras.application.SolicitudDeFacturaDeCuenta;
import com.regenta.compras.application.SolicitudDeFacturaDeRecepcion;
import com.regenta.compras.application.PagoDelNegocio;
import com.regenta.compras.application.SolicitudDePago;
import com.regenta.compras.application.SolicitudDePagoAProveedor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cuentas por pagar y pagos a proveedores. HU-049, HU-145, HU-170. */
@RestController
@RequestMapping("/api/compras/cuentas-por-pagar")
@Tag(name = "Cuentas por pagar", description = "Lo que se le debe a cada proveedor y lo que se le paga")
public class CuentasPorPagarControlador {

    private final GestionDeCuentasPorPagar cuentas;

    public CuentasPorPagarControlador(GestionDeCuentasPorPagar cuentas) {
        this.cuentas = cuentas;
    }

    @GetMapping
    @Operation(summary = "Las cuentas por pagar, por antigüedad; las vencidas quedan marcadas",
            description = "`enEsperaDeFactura=true` deja solo las que llegaron con remisión (HU-170).")
    public List<CuentaDelNegocio> listar(
            @RequestParam(name = "soloVencidas", defaultValue = "false") boolean soloVencidas,
            @RequestParam(name = "enEsperaDeFactura", defaultValue = "false") boolean enEsperaDeFactura) {
        return cuentas.listar(soloVencidas, enEsperaDeFactura);
    }

    @GetMapping("/{cuentaId}")
    @Operation(summary = "Una cuenta por pagar con sus pagos")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public CuentaDelNegocio ver(@PathVariable UUID cuentaId) {
        return cuentas.ver(cuentaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra la factura del proveedor sobre una recepción confirmada",
            description = "Si la recepción llegó con remisión, completa su cuenta en espera de factura.")
    @ApiResponse(responseCode = "409", description = "Factura repetida para el proveedor o recepción ya facturada")
    public CuentaDelNegocio registrarFactura(
            @Valid @RequestBody SolicitudDeFacturaDeRecepcion solicitud) {
        return cuentas.registrarFactura(solicitud);
    }

    @PutMapping("/{cuentaId}/factura")
    @Operation(summary = "Registra la factura que llegó después de la mercancía (HU-170)",
            description = "Quita la marca de espera y recalcula el vencimiento desde la fecha de la factura.")
    @ApiResponse(responseCode = "409", description = "La cuenta ya tiene factura, o el número ya existe para el proveedor")
    public CuentaDelNegocio registrarFacturaDeCuenta(@PathVariable UUID cuentaId,
            @Valid @RequestBody SolicitudDeFacturaDeCuenta solicitud) {
        return cuentas.registrarFacturaDeCuenta(cuentaId, solicitud);
    }

    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Un pago a un proveedor que abona a varias de sus facturas (HU-171)",
            description = "Sin `aplicaciones`, se reparte de la factura más antigua a la más nueva.")
    @ApiResponse(responseCode = "409", description = "Se pidió pagar desde la caja y el usuario no tiene una abierta")
    @ApiResponse(responseCode = "422", description = "Más de lo que se debe, reparto que no suma o cuenta de otro proveedor")
    public PagoDelNegocio pagarAProveedor(@Valid @RequestBody SolicitudDePagoAProveedor solicitud) {
        return cuentas.pagarAProveedor(solicitud);
    }

    @PostMapping("/{cuentaId}/pagos")
    @Operation(summary = "Registra un pago: baja el saldo y mueve el estado",
            description = "En efectivo con `sesionCajaId`, la salida queda registrada en esa caja (HU-145).")
    @ApiResponse(responseCode = "409", description = "La sesión de caja indicada no está abierta")
    @ApiResponse(responseCode = "422", description = "El pago supera el saldo")
    public CuentaDelNegocio registrarPago(@PathVariable UUID cuentaId,
            @Valid @RequestBody SolicitudDePago solicitud) {
        return cuentas.registrarPago(cuentaId, solicitud);
    }
}
