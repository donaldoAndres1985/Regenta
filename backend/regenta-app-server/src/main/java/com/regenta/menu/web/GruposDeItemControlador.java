package com.regenta.menu.web;

import com.regenta.menu.api.*;

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

import com.regenta.menu.application.CotizacionDeModificadores;
import com.regenta.menu.application.GestionDeModificadores;
import com.regenta.menu.application.GrupoConOpciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** QuÃ© grupos de modificadores aplican a un Ã­tem, y la validaciÃ³n de una selecciÃ³n. HU-078. */
@RestController
@RequestMapping("/api/menu/items/{itemId}")
@Tag(name = "Modificadores de Ã­tem", description = "Grupos que aplican a un plato y su cotizaciÃ³n")
public class GruposDeItemControlador {

    private final GestionDeModificadores modificadores;

    public GruposDeItemControlador(GestionDeModificadores modificadores) {
        this.modificadores = modificadores;
    }

    @GetMapping("/grupos-modificadores")
    @Operation(summary = "Los grupos de modificadores que aplican al Ã­tem, con sus opciones")
    public List<GrupoConOpciones> grupos(@PathVariable UUID itemId) {
        return modificadores.gruposDeItem(itemId);
    }

    @PutMapping("/grupos-modificadores/{grupoId}")
    @Operation(summary = "Vincula un grupo de modificadores al Ã­tem")
    public List<GrupoConOpciones> vincular(@PathVariable UUID itemId, @PathVariable UUID grupoId,
            @RequestBody(required = false) Map<String, Integer> cuerpo) {
        Integer orden = cuerpo == null ? null : cuerpo.get("orden");
        return modificadores.vincular(itemId, grupoId, orden);
    }

    @DeleteMapping("/grupos-modificadores/{grupoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desvincula un grupo del Ã­tem")
    public void desvincular(@PathVariable UUID itemId, @PathVariable UUID grupoId) {
        modificadores.desvincular(itemId, grupoId);
    }

    @PostMapping("/seleccion-modificadores")
    @Operation(summary = "Valida una selecciÃ³n de modificadores y devuelve cuÃ¡nto suma a la lÃ­nea")
    @ApiResponse(responseCode = "422", description = "La selecciÃ³n no respeta los mÃ­nimos/mÃ¡ximos")
    public CotizacionDeModificadores cotizar(@PathVariable UUID itemId,
            @RequestBody List<UUID> modificadorIds) {
        return modificadores.cotizarSeleccion(itemId, modificadorIds);
    }
}
