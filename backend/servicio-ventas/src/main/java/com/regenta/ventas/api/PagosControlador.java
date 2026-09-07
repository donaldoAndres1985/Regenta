package com.regenta.ventas.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.aplicacion.GestionDePagos;
import com.regenta.ventas.aplicacion.ResultadoDePago;
import com.regenta.ventas.aplicacion.SolicitudDePago;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cobro de una venta. HU-039. */
@RestController
@RequestMapping("/api/ventas/{ventaId}")
@Tag(name = "Pagos", description = "Registrar el pago de una venta, incluso mixto")
public class PagosControlador {

    private final GestionDePagos pagos;

    public PagosControlador(GestionDePagos pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un pago; el efectivo mayor al saldo devuelve el cambio")
    @ApiResponse(responseCode = "409", description = "La venta no está confirmada")
    @ApiResponse(responseCode = "422", description = "Crédito sin cliente, o el pago supera el saldo")
    public ResultadoDePago registrar(@PathVariable UUID ventaId,
            @Valid @RequestBody SolicitudDePago solicitud) {
        return pagos.registrarPago(ventaId, solicitud);
    }

    @PostMapping("/cierre")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra la venta; exige que los pagos cubran el total")
    @ApiResponse(responseCode = "422", description = "Faltan pagos para cubrir el total")
    public void cerrar(@PathVariable UUID ventaId) {
        pagos.cerrarVenta(ventaId);
    }
}
