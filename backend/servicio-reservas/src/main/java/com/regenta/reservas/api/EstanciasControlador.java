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

import com.regenta.reservas.aplicacion.EstanciaDelNegocio;
import com.regenta.reservas.aplicacion.GestionDeEstancias;
import com.regenta.reservas.aplicacion.OcupanteDelNegocio;
import com.regenta.reservas.aplicacion.SolicitudDeCheckIn;
import com.regenta.reservas.aplicacion.SolicitudDeConsumo;
import com.regenta.reservas.aplicacion.SolicitudDeOcupante;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Check-in, estancia y ocupantes. HU-072. */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Estancias", description = "Check-in, asignación de habitación y ocupantes")
public class EstanciasControlador {

    private final GestionDeEstancias estancias;

    public EstanciasControlador(GestionDeEstancias estancias) {
        this.estancias = estancias;
    }

    @PostMapping("/{reservaId}/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra el check-in, asigna la habitación y abre la estancia")
    @ApiResponse(responseCode = "409", description = "La reserva no está confirmada, ya tiene "
            + "check-in, o el recurso está ocupado")
    @ApiResponse(responseCode = "422", description = "El titular no trae documento")
    public EstanciaDelNegocio checkIn(@PathVariable UUID reservaId,
            @Valid @RequestBody SolicitudDeCheckIn solicitud) {
        return estancias.checkIn(reservaId, solicitud);
    }

    @GetMapping("/{reservaId}/estancia")
    @Operation(summary = "La estancia de una reserva, con sus ocupantes")
    @ApiResponse(responseCode = "404", description = "La reserva no tiene estancia")
    public EstanciaDelNegocio verEstancia(@PathVariable UUID reservaId) {
        return estancias.verEstancia(reservaId);
    }

    @PostMapping("/{reservaId}/ocupantes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega un ocupante a la reserva")
    public OcupanteDelNegocio agregarOcupante(@PathVariable UUID reservaId,
            @Valid @RequestBody SolicitudDeOcupante solicitud) {
        return estancias.agregarOcupante(reservaId, solicitud);
    }

    @PostMapping("/{reservaId}/consumos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Carga un consumo a la habitación (minibar, restaurante, spa…)")
    @ApiResponse(responseCode = "409", description = "La estancia ya está cerrada")
    public EstanciaDelNegocio cargarConsumo(@PathVariable UUID reservaId,
            @Valid @RequestBody SolicitudDeConsumo solicitud) {
        return estancias.cargarConsumo(reservaId, solicitud);
    }
}
