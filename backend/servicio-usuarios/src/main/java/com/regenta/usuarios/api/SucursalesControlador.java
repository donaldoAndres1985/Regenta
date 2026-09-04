package com.regenta.usuarios.api;

import java.util.List;
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

import com.regenta.usuarios.aplicacion.GestionDeSucursales;
import com.regenta.usuarios.aplicacion.SolicitudDeSucursal;
import com.regenta.usuarios.aplicacion.SucursalDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Sucursales. HU-019. */
@RestController
@RequestMapping("/api/usuarios/sucursales")
@Tag(name = "Sucursales", description = "Los puntos fisicos del negocio")
public class SucursalesControlador {

    private final GestionDeSucursales gestion;

    public SucursalesControlador(GestionDeSucursales gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    @Operation(summary = "Las sucursales del negocio")
    public List<SucursalDelNegocio> listar() {
        return gestion.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una sucursal")
    @ApiResponse(responseCode = "402", description = "El plan no incluye multi-sucursal")
    public SucursalDelNegocio crear(@Valid @RequestBody SolicitudDeSucursal solicitud) {
        return gestion.crear(solicitud);
    }

    @PutMapping("/{sucursalId}")
    @Operation(summary = "Cambia los datos de una sucursal")
    public SucursalDelNegocio actualizar(@PathVariable UUID sucursalId,
            @Valid @RequestBody SolicitudDeSucursal solicitud) {
        return gestion.actualizar(sucursalId, solicitud);
    }

    @PostMapping("/{sucursalId}/principal")
    @Operation(summary = "La marca como principal. La anterior deja de serlo")
    public SucursalDelNegocio marcarPrincipal(@PathVariable UUID sucursalId) {
        return gestion.marcarPrincipal(sucursalId);
    }

    @DeleteMapping("/{sucursalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactiva una sucursal que no sea la principal")
    public void desactivar(@PathVariable UUID sucursalId) {
        gestion.desactivar(sucursalId);
    }
}
