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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.menu.aplicacion.GestionDeItemsDeMenu;
import com.regenta.menu.aplicacion.ItemDelNegocio;
import com.regenta.menu.aplicacion.SolicitudDeItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ítems de menú. HU-077. */
@RestController
@RequestMapping("/api/menu/items")
@Tag(name = "Ítems de menú", description = "Platos, bebidas y adicionales de la carta")
public class ItemsDeMenuControlador {

    private final GestionDeItemsDeMenu items;

    public ItemsDeMenuControlador(GestionDeItemsDeMenu items) {
        this.items = items;
    }

    @GetMapping
    @Operation(summary = "Los ítems de una categoría")
    public List<ItemDelNegocio> listar(@RequestParam UUID categoriaId) {
        return items.listarPorCategoria(categoriaId);
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Un ítem por id")
    public ItemDelNegocio ver(@PathVariable UUID itemId) {
        return items.ver(itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un ítem con su categoría, precio, estación y tiempo de preparación")
    @ApiResponse(responseCode = "409", description = "Ya hay un ítem con ese código")
    public ItemDelNegocio crear(@Valid @RequestBody SolicitudDeItem solicitud) {
        return items.crear(solicitud);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Cambia un ítem")
    public ItemDelNegocio actualizar(@PathVariable UUID itemId,
            @Valid @RequestBody SolicitudDeItem solicitud) {
        return items.actualizar(itemId, solicitud);
    }

    @PostMapping("/{itemId}/disponibilidad")
    @Operation(summary = "Marca el ítem disponible o agotado (\"se acabó\")")
    public ItemDelNegocio disponibilidad(@PathVariable UUID itemId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return items.cambiarDisponibilidad(itemId, Boolean.TRUE.equals(cuerpo.get("disponible")));
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un ítem (soft-delete)")
    public void eliminar(@PathVariable UUID itemId) {
        items.eliminar(itemId);
    }
}
