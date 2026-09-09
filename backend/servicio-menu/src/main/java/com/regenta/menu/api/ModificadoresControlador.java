package com.regenta.menu.api;

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

import com.regenta.menu.aplicacion.GestionDeModificadores;
import com.regenta.menu.aplicacion.GrupoConOpciones;
import com.regenta.menu.aplicacion.GrupoDelNegocio;
import com.regenta.menu.aplicacion.ModificadorDelNegocio;
import com.regenta.menu.aplicacion.SolicitudDeGrupo;
import com.regenta.menu.aplicacion.SolicitudDeModificador;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Grupos de modificadores y sus opciones. HU-078. */
@RestController
@RequestMapping("/api/menu/modificadores")
@Tag(name = "Modificadores", description = "Opciones y adiciones por plato, con mínimos y máximos")
public class ModificadoresControlador {

    private final GestionDeModificadores modificadores;

    public ModificadoresControlador(GestionDeModificadores modificadores) {
        this.modificadores = modificadores;
    }

    @GetMapping("/grupos")
    @Operation(summary = "Los grupos de modificadores del negocio, con sus opciones")
    public List<GrupoConOpciones> listarGrupos() {
        return modificadores.listarGrupos();
    }

    @GetMapping("/grupos/{grupoId}")
    @Operation(summary = "Un grupo con sus opciones")
    public GrupoConOpciones verGrupo(@PathVariable UUID grupoId) {
        return modificadores.verGrupo(grupoId);
    }

    @PostMapping("/grupos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un grupo de modificadores")
    @ApiResponse(responseCode = "422", description = "El máximo no puede ser menor que el mínimo")
    public GrupoDelNegocio crearGrupo(@Valid @RequestBody SolicitudDeGrupo solicitud) {
        return modificadores.crearGrupo(solicitud);
    }

    @PutMapping("/grupos/{grupoId}")
    @Operation(summary = "Cambia un grupo")
    public GrupoDelNegocio actualizarGrupo(@PathVariable UUID grupoId,
            @Valid @RequestBody SolicitudDeGrupo solicitud) {
        return modificadores.actualizarGrupo(grupoId, solicitud);
    }

    @PostMapping("/grupos/{grupoId}/activacion")
    @Operation(summary = "Activa o desactiva el grupo")
    public GrupoDelNegocio activacionGrupo(@PathVariable UUID grupoId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return modificadores.cambiarActivacionGrupo(grupoId, Boolean.TRUE.equals(cuerpo.get("activo")));
    }

    @DeleteMapping("/grupos/{grupoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un grupo (y sus opciones y vínculos)")
    public void eliminarGrupo(@PathVariable UUID grupoId) {
        modificadores.eliminarGrupo(grupoId);
    }

    @PostMapping("/grupos/{grupoId}/opciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una opción al grupo")
    public ModificadorDelNegocio agregarOpcion(@PathVariable UUID grupoId,
            @Valid @RequestBody SolicitudDeModificador solicitud) {
        return modificadores.agregarModificador(grupoId, solicitud);
    }

    @PutMapping("/opciones/{modificadorId}")
    @Operation(summary = "Cambia una opción")
    public ModificadorDelNegocio actualizarOpcion(@PathVariable UUID modificadorId,
            @Valid @RequestBody SolicitudDeModificador solicitud) {
        return modificadores.actualizarModificador(modificadorId, solicitud);
    }

    @PostMapping("/opciones/{modificadorId}/activacion")
    @Operation(summary = "Activa o desactiva una opción")
    public ModificadorDelNegocio activacionOpcion(@PathVariable UUID modificadorId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return modificadores.cambiarActivacionModificador(modificadorId,
                Boolean.TRUE.equals(cuerpo.get("activo")));
    }

    @DeleteMapping("/opciones/{modificadorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita una opción del grupo")
    public void quitarOpcion(@PathVariable UUID modificadorId) {
        modificadores.quitarModificador(modificadorId);
    }
}
