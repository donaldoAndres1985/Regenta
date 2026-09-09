package com.regenta.menu.api;

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

import com.regenta.menu.aplicacion.ExplosionDeRecetas;
import com.regenta.menu.aplicacion.GestionDeRecetas;
import com.regenta.menu.aplicacion.InsumoAConsumir;
import com.regenta.menu.aplicacion.LineaAExplotar;
import com.regenta.menu.aplicacion.RecetaDelItem;
import com.regenta.menu.aplicacion.SolicitudDeLineaDeReceta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Recetas: qué insumos consume cada ítem y la explosión de líneas de comanda. HU-079. */
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
    @Operation(summary = "La receta de un ítem y su costo estimado")
    public RecetaDelItem ver(@PathVariable UUID itemMenuId) {
        return recetas.ver(itemMenuId);
    }

    @PutMapping("/items/{itemMenuId}/receta")
    @Operation(summary = "Reemplaza la receta del ítem y recalcula su costo estimado")
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
    @Operation(summary = "Cambia una línea de receta")
    public RecetaDelItem actualizarLinea(@PathVariable UUID lineaId,
            @Valid @RequestBody SolicitudDeLineaDeReceta solicitud) {
        return recetas.actualizarLinea(lineaId, solicitud);
    }

    @DeleteMapping("/items/receta/lineas/{lineaId}")
    @Operation(summary = "Quita una línea de la receta")
    public RecetaDelItem quitarLinea(@PathVariable UUID lineaId) {
        return recetas.quitarLinea(lineaId);
    }

    @PostMapping("/items/{itemMenuId}/receta/recalculo")
    @Operation(summary = "Recalcula el costo estimado del ítem contra los costos de inventario")
    public RecetaDelItem recalcular(@PathVariable UUID itemMenuId) {
        return recetas.recalcularCosto(itemMenuId);
    }

    @PostMapping("/recetas/explosion")
    @Operation(summary = "Explota líneas de comanda contra sus recetas: qué insumos descontar")
    public List<InsumoAConsumir> explotar(@RequestBody List<@Valid LineaAExplotar> lineas) {
        return explosion.explotar(lineas);
    }
}
