package com.regenta.reservas.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.reservas.aplicacion.GestionDeReservas;
import com.regenta.reservas.aplicacion.ReservaDelNegocio;
import com.regenta.reservas.aplicacion.SolicitudDeCancelacion;
import com.regenta.reservas.aplicacion.SolicitudDePagoDeReserva;
import com.regenta.reservas.aplicacion.SolicitudDeReserva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Reservas de recursos y su ciclo de estados. HU-070 · HU-071. */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Apartar un recurso para un periodo y moverlo por sus estados")
public class ReservasControlador {

    private final GestionDeReservas reservas;

    public ReservasControlador(GestionDeReservas reservas) {
        this.reservas = reservas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una reserva; cotiza el periodo y calcula el anticipo requerido")
    @ApiResponse(responseCode = "409", description = "El recurso ya está reservado en ese periodo")
    @ApiResponse(responseCode = "422", description = "El periodo no tiene un inicio y un fin válidos")
    public ReservaDelNegocio crear(@Valid @RequestBody SolicitudDeReserva solicitud) {
        return reservas.crear(solicitud);
    }

    @GetMapping("/{reservaId}")
    @Operation(summary = "Una reserva por id")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ReservaDelNegocio ver(@PathVariable UUID reservaId) {
        return reservas.ver(reservaId);
    }

    @PostMapping("/{reservaId}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un pago sobre la reserva (anticipo, saldo, depósito…)")
    public ReservaDelNegocio registrarPago(@PathVariable UUID reservaId,
            @Valid @RequestBody SolicitudDePagoDeReserva solicitud) {
        return reservas.registrarPago(reservaId, solicitud);
    }

    @PostMapping("/{reservaId}/confirmacion")
    @Operation(summary = "Confirma la reserva pendiente; exige el anticipo cobrado")
    @ApiResponse(responseCode = "409", description = "No está pendiente o falta el anticipo")
    public ReservaDelNegocio confirmar(@PathVariable UUID reservaId) {
        return reservas.confirmar(reservaId);
    }

    @PostMapping("/{reservaId}/cancelacion")
    @Operation(summary = "Cancela la reserva y calcula la penalización de la política")
    @ApiResponse(responseCode = "409", description = "La reserva no se puede cancelar en su estado")
    public ReservaDelNegocio cancelar(@PathVariable UUID reservaId,
            @RequestBody(required = false) @Valid SolicitudDeCancelacion solicitud) {
        return reservas.cancelar(reservaId, solicitud);
    }

    @PostMapping("/{reservaId}/no-show")
    @Operation(summary = "Marca como no-show una reserva confirmada")
    @ApiResponse(responseCode = "409", description = "Solo una reserva confirmada puede ser no-show")
    public ReservaDelNegocio marcarNoShow(@PathVariable UUID reservaId) {
        return reservas.marcarNoShow(reservaId);
    }
}
