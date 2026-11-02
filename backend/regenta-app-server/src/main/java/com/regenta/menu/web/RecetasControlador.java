package com.regenta.menu.web;

import com.regenta.menu.api.*;

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

import com.regenta.menu.application.ExplosionDeRecetas;
import com.regenta.menu.application.GestionDeRecetas;
import com.regenta.menu.application.InsumoAConsumir;
import com.regenta.menu.application.LineaAExplotar;
import com.regenta.menu.application.RecetaDelItem;
import com.regenta.menu.application.SolicitudDeLineaDeReceta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Recetas: quÃ© insumos consume cada Ã­tem y la explosiÃ³n de lÃ­neas de comanda. HU-079. */
@RestController
@RequestMapping("/api/menu")
@Tag(name = "Recetas", description = "El puente entre la comanda y el inventario")
public class RecetasControlador {

    private final GestionDeRecetas recetas;
    private final ExplosionDeRecetas explosion;

    public RecetasControlador(GestionDeRecetas recetas, ExplosionDeRecetas explosion) {
        this.recetas = recetas;
        this.explosion = explosion;
    }

    @GetMapping("/items/{itemMenuId}/receta")
    @Operation(summary = "La receta de un Ã­tem y su costo estimado")
    public RecetaDelItem ver(@PathVariable UUID itemMenuId) {
        return recetas.ver(itemMenuId);
    }

    @PutMapping("/items/{itemMenuId}/receta")
    @Operation(summary = "Reemplaza la receta del Ã­tem y recalcula su costo estimado")
    public RecetaDelItem reemplazar(@PathVariable UUID itemMenuId,
            @RequestBody List<@Valid SolicitudDeLineaDeReceta> lineas) {
        return recetas.reemplazar(itemMenuId, lineas);
    }

    @PostMapping("/items/{itemMenuId}/receta/lineas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega un insumo a la receta")
    public RecetaDelItem agregarLinea(@PathVariable UUID itemMenuId,
            @Valid @RequestBody SolicitudDeLineaDeReceta solicitud) {
        return recetas.agregarLinea(itemMenuId, solicitud);
    }

    @PutMapping("/items/receta/lineas/{lineaId}")
    @Operation(summary = "Cambia una lÃ­nea de receta")
    public RecetaDelItem actualizarLinea(@PathVariable UUID lineaId,
            @Valid @RequestBody SolicitudDeLineaDeReceta solicitud) {
        return recetas.actualizarLinea(lineaId, solicitud);
    }

    @DeleteMapping("/items/receta/lineas/{lineaId}")
    @Operation(summary = "Quita una lÃ­nea de la receta")
    public RecetaDelItem quitarLinea(@PathVariable UUID lineaId) {
        return recetas.quitarLinea(lineaId);
    }

    @PostMapping("/items/{itemMenuId}/receta/recalculo")
    @Operation(summary = "Recalcula el costo estimado del Ã­tem contra los costos de inventario")
    public RecetaDelItem recalcular(@PathVariable UUID itemMenuId) {
        return recetas.recalcularCosto(itemMenuId);
    }

    @PostMapping("/recetas/explosion")
    @Operation(summary = "Explota lÃ­neas de comanda contra sus recetas: quÃ© insumos descontar")
    public List<InsumoAConsumir> explotar(@RequestBody List<@Valid LineaAExplotar> lineas) {
        return explosion.explotar(lineas);
    }
}
