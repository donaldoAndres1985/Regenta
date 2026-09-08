package com.regenta.recursos.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.recursos.aplicacion.GestionDeRecursos;
import com.regenta.recursos.aplicacion.RecursoDelNegocio;
import com.regenta.recursos.aplicacion.SolicitudDeRecurso;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Recursos individuales del negocio. HU-065. */
@RestController
@RequestMapping("/api/recursos")
@Tag(name = "Recursos", description = "Cada habitación, cancha o consultorio reservable")
public class RecursosControlador {

    private final GestionDeRecursos recursos;

    public RecursosControlador(GestionDeRecursos recursos) {
        this.recursos = recursos;
    }

    @GetMapping
    @Operation(summary = "Los recursos del negocio; con ?soloDisponibles=true, solo los reservables")
    public List<RecursoDelNegocio> listar(
            @RequestParam(name = "soloDisponibles", defaultValue = "false") boolean soloDisponibles) {
        return recursos.listar(soloDisponibles);
    }

    @GetMapping("/{recursoId}")
    @Operation(summary = "Un recurso por id")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public RecursoDelNegocio ver(@PathVariable UUID recursoId) {
        return recursos.ver(recursoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da de alta un recurso; valida sus atributos contra su tipo")
    @ApiResponse(responseCode = "409", description = "Ya hay un recurso con ese código")
    @ApiResponse(responseCode = "422", description = "Los atributos no cumplen el contrato del tipo")
    public RecursoDelNegocio crear(@Valid @RequestBody SolicitudDeRecurso solicitud) {
        return recursos.crear(solicitud);
    }

    @PutMapping("/{recursoId}")
    @Operation(summary = "Cambia los datos y atributos de un recurso")
    public RecursoDelNegocio actualizar(@PathVariable UUID recursoId,
            @Valid @RequestBody SolicitudDeRecurso solicitud) {
        return recursos.actualizar(recursoId, solicitud);
    }

    @PatchMapping("/{recursoId}/estado")
    @Operation(summary = "Cambia el estado operativo (DISPONIBLE, MANTENIMIENTO, …)")
    public RecursoDelNegocio cambiarEstado(@PathVariable UUID recursoId,
            @RequestBody Map<String, String> cuerpo) {
        return recursos.cambiarEstado(recursoId, cuerpo.get("estado"));
    }

    @DeleteMapping("/{recursoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un recurso (no si tiene reservas futuras)")
    @ApiResponse(responseCode = "409", description = "El recurso tiene reservas futuras")
    public void eliminar(@PathVariable UUID recursoId) {
        recursos.eliminar(recursoId);
    }
}
