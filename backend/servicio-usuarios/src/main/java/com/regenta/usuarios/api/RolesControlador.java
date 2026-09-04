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

import com.regenta.usuarios.aplicacion.GestionDeRoles;
import com.regenta.usuarios.aplicacion.PermisoDelCatalogo;
import com.regenta.usuarios.aplicacion.RolDelNegocio;
import com.regenta.usuarios.aplicacion.SolicitudDeRol;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Roles y catalogo de permisos. HU-016. */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Roles", description = "Roles del negocio y catalogo de permisos")
public class RolesControlador {

    private final GestionDeRoles gestion;

    public RolesControlador(GestionDeRoles gestion) {
        this.gestion = gestion;
    }

    @GetMapping("/roles")
    @Operation(summary = "Los roles del negocio, con cuanta gente tiene cada uno")
    public List<RolDelNegocio> listar() {
        return gestion.listar();
    }

    @GetMapping("/permisos")
    @Operation(summary = "El catalogo global de permisos")
    public List<PermisoDelCatalogo> permisos() {
        return gestion.catalogoDePermisos();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un rol con el subconjunto de permisos que se le de")
    public RolDelNegocio crear(@Valid @RequestBody SolicitudDeRol solicitud) {
        return gestion.crear(solicitud);
    }

    @PutMapping("/roles/{rolId}")
    @Operation(summary = "Cambia el nombre o los permisos de un rol")
    public RolDelNegocio actualizar(@PathVariable UUID rolId,
            @Valid @RequestBody SolicitudDeRol solicitud) {
        return gestion.actualizar(rolId, solicitud);
    }

    @DeleteMapping("/roles/{rolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un rol")
    @ApiResponse(responseCode = "409", description = "Es de sistema, o todavia tiene gente")
    public void eliminar(@PathVariable UUID rolId) {
        gestion.eliminar(rolId);
    }
}
