package com.regenta.menu.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.menu.aplicacion.EstacionDelNegocio;
import com.regenta.menu.aplicacion.GestionDeEstacionesDeCocina;
import com.regenta.menu.aplicacion.SolicitudDeEstacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Estaciones de cocina. HU-077. */
@RestController
@RequestMapping("/api/menu/estaciones")
@Tag(name = "Estaciones de cocina", description = "Parrilla, fría, bar: a dónde va cada comanda")
public class EstacionesDeCocinaControlador {

    private final GestionDeEstacionesDeCocina estaciones;

    public EstacionesDeCocinaControlador(GestionDeEstacionesDeCocina estaciones) {
        this.estaciones = estaciones;
    }

    @GetMapping
    @Operation(summary = "Las estaciones de cocina del negocio")
    public List<EstacionDelNegocio> listar() {
        return estaciones.listar();
    }

    @GetMapping("/{estacionId}")
    @Operation(summary = "Una estación por id")
    public EstacionDelNegocio ver(@PathVariable UUID estacionId) {
        return estaciones.ver(estacionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una estación de cocina")
    @ApiResponse(responseCode = "409", description = "Ya hay una estación con ese código")
    public EstacionDelNegocio crear(@Valid @RequestBody SolicitudDeEstacion solicitud) {
        return estaciones.crear(solicitud);
    }

    @PutMapping("/{estacionId}")
    @Operation(summary = "Cambia una estación")
    public EstacionDelNegocio actualizar(@PathVariable UUID estacionId,
            @Valid @RequestBody SolicitudDeEstacion solicitud) {
        return estaciones.actualizar(estacionId, solicitud);
    }

    @PostMapping("/{estacionId}/activacion")
    @Operation(summary = "Activa o desactiva la estación")
    public EstacionDelNegocio activacion(@PathVariable UUID estacionId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return estaciones.cambiarActivacion(estacionId, Boolean.TRUE.equals(cuerpo.get("activa")));
    }
}
