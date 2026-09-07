package com.regenta.clientes.api;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.aplicacion.CarteraDelCliente;
import com.regenta.clientes.aplicacion.GestionDeCartera;
import com.regenta.clientes.aplicacion.ResultadoDeCupo;
import com.regenta.clientes.aplicacion.SolicitudDeCredito;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cupo de crédito y cartera del cliente. HU-022. */
@RestController
@RequestMapping("/api/clientes/{clienteId}")
@Tag(name = "Cartera", description = "Cupo de crédito, saldo y cuentas por cobrar del cliente")
public class CarteraControlador {

    private final GestionDeCartera cartera;

    public CarteraControlador(GestionDeCartera cartera) {
        this.cartera = cartera;
    }

    @PutMapping("/credito")
    @Operation(summary = "Habilita el crédito del cliente y fija su cupo y días de plazo")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    public CarteraDelCliente configurarCredito(@PathVariable UUID clienteId,
            @Valid @RequestBody SolicitudDeCredito solicitud) {
        return cartera.configurarCredito(clienteId, solicitud);
    }

    @GetMapping("/cartera")
    @Operation(summary = "Cupo, saldo, disponible y cuentas por cobrar con sus días de mora")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    public CarteraDelCliente verCartera(@PathVariable UUID clienteId) {
        return cartera.verCartera(clienteId);
    }

    @PostMapping("/cupo/validacion")
    @Operation(summary = "¿Cabe una venta a crédito de este monto en el cupo del cliente?")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    public ResultadoDeCupo validarCupo(@PathVariable UUID clienteId,
            @RequestBody Map<String, BigDecimal> cuerpo) {
        return cartera.validarCupo(clienteId, cuerpo.get("monto"));
    }
}
