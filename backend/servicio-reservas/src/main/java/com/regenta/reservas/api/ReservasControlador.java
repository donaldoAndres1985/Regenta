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
import com.regenta.reservas.aplicacion.SolicitudDeReserva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Reservas de recursos. HU-070. */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Apartar un recurso para un periodo, sin overbooking")
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
}
