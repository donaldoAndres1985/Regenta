package com.regenta.usuarios.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.usuarios.aplicacion.GestionDeModulos;
import com.regenta.usuarios.aplicacion.ModuloActivo;
import com.regenta.usuarios.aplicacion.ResumenDelNegocio;
import com.regenta.usuarios.aplicacion.SolicitudDeCambioDePlan;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** El plan y los modulos del negocio. HU-017 y HU-020. */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Plan y modulos", description = "Que tiene encendido el negocio y con que plan")
public class ModulosControlador {

    private final GestionDeModulos gestion;

    public ModulosControlador(GestionDeModulos gestion) {
        this.gestion = gestion;
    }

    @GetMapping("/mi-negocio")
    @Operation(summary = "Plan, patron, limites y modulos activos del negocio del token")
    public ResumenDelNegocio miNegocio() {
        return gestion.miNegocio();
    }

    @GetMapping("/modulos")
    @Operation(summary = "Los modulos encendidos, con el origen de cada uno")
    public List<ModuloActivo> modulos() {
        return gestion.activos();
    }

    @PostMapping("/modulos/{codigo}/activar")
    @Operation(summary = "Enciende un modulo como add-on, sin cambiar de plan")
    @ApiResponse(responseCode = "422", description = "Le faltan modulos de los que depende")
    public ModuloActivo activar(@PathVariable String codigo) {
        return gestion.activarComoAddon(codigo);
    }

    @DeleteMapping("/modulos/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Apaga un modulo, si nadie depende de el")
    public void desactivar(@PathVariable String codigo) {
        gestion.desactivar(codigo);
    }

    @PostMapping("/negocios/plan")
    @Operation(summary = "Cambia el plan del negocio y recalcula sus modulos")
    public ResumenDelNegocio cambiarDePlan(@Valid @RequestBody SolicitudDeCambioDePlan solicitud) {
        return gestion.cambiarDePlan(solicitud);
    }
}
