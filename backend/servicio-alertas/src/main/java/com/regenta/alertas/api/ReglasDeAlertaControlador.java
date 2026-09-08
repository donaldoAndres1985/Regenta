package com.regenta.alertas.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.alertas.aplicacion.GestionDeReglas;
import com.regenta.alertas.aplicacion.HechoDeAlerta;
import com.regenta.alertas.aplicacion.ReglaDelNegocio;
import com.regenta.alertas.aplicacion.SolicitudDeRegla;
import com.regenta.alertas.aplicacion.TipoDeAlerta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Reglas de alerta configurables. HU-092. */
@RestController
@RequestMapping("/api/alertas/reglas")
@Tag(name = "Reglas de alerta", description = "Un motor de reglas, no condiciones en el código")
public class ReglasDeAlertaControlador {

    private final GestionDeReglas reglas;

    public ReglasDeAlertaControlador(GestionDeReglas reglas) {
        this.reglas = reglas;
    }

    @GetMapping("/tipos")
    @Operation(summary = "El catálogo de tipos de alerta para armar una regla")
    public List<TipoDeAlerta> tipos() {
        return reglas.tiposDisponibles();
    }

    @GetMapping
    @Operation(summary = "Las reglas de alerta del negocio")
    public List<ReglaDelNegocio> listar() {
        return reglas.listar();
    }

    @GetMapping("/{reglaId}")
    @Operation(summary = "Una regla por id")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ReglaDelNegocio ver(@PathVariable UUID reglaId) {
        return reglas.ver(reglaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una regla: tipo, condición, severidad, canales y destinatarios")
    @ApiResponse(responseCode = "409", description = "Ya hay una regla con ese nombre para el tipo")
    public ReglaDelNegocio crear(@Valid @RequestBody SolicitudDeRegla solicitud) {
        return reglas.crear(solicitud);
    }

    @PutMapping("/{reglaId}")
    @Operation(summary = "Cambia la condición y los parámetros de una regla")
    public ReglaDelNegocio actualizar(@PathVariable UUID reglaId,
            @Valid @RequestBody SolicitudDeRegla solicitud) {
        return reglas.actualizar(reglaId, solicitud);
    }

    @PatchMapping("/{reglaId}/activa")
    @Operation(summary = "Activa o desactiva una regla")
    public ReglaDelNegocio cambiarActiva(@PathVariable UUID reglaId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return reglas.cambiarActiva(reglaId, Boolean.TRUE.equals(cuerpo.get("activa")));
    }

    @PostMapping("/evaluacion")
    @Operation(summary = "Evalúa un hecho contra las reglas activas del tipo y genera alertas")
    public List<UUID> evaluar(@Valid @RequestBody HechoDeAlerta hecho) {
        return reglas.evaluar(hecho);
    }
}
