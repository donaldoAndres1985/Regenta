package com.regenta.clientes.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.aplicacion.CuentaEnCartera;
import com.regenta.clientes.aplicacion.GestionDeCartera;
import com.regenta.clientes.aplicacion.SolicitudDeRecaudo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Recaudos contra las cuentas por cobrar. HU-022. */
@RestController
@RequestMapping("/api/clientes/cuentas")
@Tag(name = "Recaudos", description = "Pagos que bajan el saldo de una cuenta por cobrar")
public class CuentasControlador {

    private final GestionDeCartera cartera;

    public CuentasControlador(GestionDeCartera cartera) {
        this.cartera = cartera;
    }

    @PostMapping("/{cuentaId}/recaudos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un pago; deja la cuenta PARCIAL o PAGADA y ajusta el saldo del cliente")
    @ApiResponse(responseCode = "404", description = "La cuenta no existe en este negocio")
    @ApiResponse(responseCode = "422", description = "El recaudo excede el saldo pendiente")
    public CuentaEnCartera registrarRecaudo(@PathVariable UUID cuentaId,
            @Valid @RequestBody SolicitudDeRecaudo solicitud) {
        return cartera.registrarRecaudo(cuentaId, solicitud);
    }
}
