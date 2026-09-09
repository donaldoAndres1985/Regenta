package com.regenta.mesas.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.mesas.aplicacion.GestionDeZonas;
import com.regenta.mesas.aplicacion.SolicitudDeZona;
import com.regenta.mesas.aplicacion.ZonaDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Zonas del salón. HU-081. */
@RestController
@RequestMapping("/api/mesas/zonas")
@Tag(name = "Zonas del salón", description = "Salón, Terraza, Barra: agrupan mesas en el plano")
public class ZonasControlador {

    private final GestionDeZonas zonas;

    public ZonasControlador(GestionDeZonas zonas) {
        this.zonas = zonas;
    }

    @GetMapping
    @Operation(summary = "Las zonas del negocio, en orden")
    public List<ZonaDelNegocio> listar() {
        return zonas.listar();
    }

    @GetMapping("/{zonaId}")
    @Operation(summary = "Una zona por id")
    public ZonaDelNegocio ver(@PathVariable UUID zonaId) {
        return zonas.ver(zonaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una zona")
    @ApiResponse(responseCode = "409", description = "Ya hay una zona con ese nombre")
    public ZonaDelNegocio crear(@Valid @RequestBody SolicitudDeZona solicitud) {
        return zonas.crear(solicitud);
    }

    @PutMapping("/{zonaId}")
    @Operation(summary = "Cambia una zona")
    public ZonaDelNegocio actualizar(@PathVariable UUID zonaId,
            @Valid @RequestBody SolicitudDeZona solicitud) {
        return zonas.actualizar(zonaId, solicitud);
    }

    @PostMapping("/{zonaId}/activacion")
    @Operation(summary = "Activa o desactiva la zona")
    public ZonaDelNegocio activacion(@PathVariable UUID zonaId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return zonas.cambiarActivacion(zonaId, Boolean.TRUE.equals(cuerpo.get("activa")));
    }

    @PutMapping("/orden")
    @Operation(summary = "Fija el orden de las zonas en el plano")
    public List<ZonaDelNegocio> reordenar(@RequestBody List<UUID> ordenIds) {
        return zonas.reordenar(ordenIds);
    }

    @DeleteMapping("/{zonaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borra una zona sin mesas")
    @ApiResponse(responseCode = "409", description = "La zona todavía tiene mesas")
    public void eliminar(@PathVariable UUID zonaId) {
        zonas.eliminar(zonaId);
    }
}
