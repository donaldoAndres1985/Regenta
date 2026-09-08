package com.regenta.recursos.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.recursos.aplicacion.BloqueoCreado;
import com.regenta.recursos.aplicacion.BloqueoDelNegocio;
import com.regenta.recursos.aplicacion.DisponibilidadDeRecurso;
import com.regenta.recursos.aplicacion.GestionDeBloqueos;
import com.regenta.recursos.aplicacion.SolicitudDeBloqueo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Bloqueos de recurso por mantenimiento. HU-067. */
@RestController
@RequestMapping("/api/recursos")
@Tag(name = "Bloqueos de recurso", description = "Sacar de circulación una habitación que está en obra")
public class BloqueosControlador {

    private final GestionDeBloqueos bloqueos;

    public BloqueosControlador(GestionDeBloqueos bloqueos) {
        this.bloqueos = bloqueos;
    }

    @GetMapping("/{recursoId}/bloqueos")
    @Operation(summary = "Los bloqueos de un recurso, del más próximo al más lejano")
    public List<BloqueoDelNegocio> listar(@PathVariable UUID recursoId) {
        return bloqueos.listar(recursoId);
    }

    @GetMapping("/{recursoId}/disponibilidad")
    @Operation(summary = "Si el recurso está libre de bloqueos entre dos instantes")
    public DisponibilidadDeRecurso disponibilidad(@PathVariable UUID recursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return bloqueos.disponibilidad(recursoId, desde, hasta);
    }

    @PostMapping("/{recursoId}/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Bloquea el recurso un periodo; avisa si pisa reservas confirmadas")
    @ApiResponse(responseCode = "409", description = "Se solapa con otro bloqueo del recurso")
    @ApiResponse(responseCode = "422", description = "El periodo no tiene un inicio y un fin válidos")
    public BloqueoCreado crear(@PathVariable UUID recursoId,
            @Valid @RequestBody SolicitudDeBloqueo solicitud) {
        return bloqueos.crear(recursoId, solicitud);
    }

    @DeleteMapping("/bloqueos/{bloqueoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Levanta un bloqueo")
    public void eliminar(@PathVariable UUID bloqueoId) {
        bloqueos.eliminar(bloqueoId);
    }
}
