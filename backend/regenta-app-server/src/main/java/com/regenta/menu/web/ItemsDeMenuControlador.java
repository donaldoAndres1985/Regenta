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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.menu.application.GestionDeItemsDeMenu;
import com.regenta.menu.application.ItemDelNegocio;
import com.regenta.menu.application.SolicitudDeItem;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Ãtems de menÃº. HU-077. */
@RestController
@RequestMapping("/api/menu/items")
@Tag(name = "Ãtems de menÃº", description = "Platos, bebidas y adicionales de la carta")
public class ItemsDeMenuControlador {

    private final GestionDeItemsDeMenu items;

    public ItemsDeMenuControlador(GestionDeItemsDeMenu items) {
        this.items = items;
    }

    @GetMapping
    @Operation(summary = "Los Ã­tems de una categorÃ­a")
    public List<ItemDelNegocio> listar(@RequestParam UUID categoriaId) {
        return items.listarPorCategoria(categoriaId);
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Un Ã­tem por id")
    public ItemDelNegocio ver(@PathVariable UUID itemId) {
        return items.ver(itemId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un Ã­tem con su categorÃ­a, precio, estaciÃ³n y tiempo de preparaciÃ³n")
    @ApiResponse(responseCode = "409", description = "Ya hay un Ã­tem con ese cÃ³digo")
    public ItemDelNegocio crear(@Valid @RequestBody SolicitudDeItem solicitud) {
        return items.crear(solicitud);
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Cambia un Ã­tem")
    public ItemDelNegocio actualizar(@PathVariable UUID itemId,
            @Valid @RequestBody SolicitudDeItem solicitud) {
        return items.actualizar(itemId, solicitud);
    }

    @PostMapping("/{itemId}/disponibilidad")
    @Operation(summary = "Marca el Ã­tem disponible o agotado (\"se acabÃ³\")")
    public ItemDelNegocio disponibilidad(@PathVariable UUID itemId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return items.cambiarDisponibilidad(itemId, Boolean.TRUE.equals(cuerpo.get("disponible")));
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un Ã­tem (soft-delete)")
    public void eliminar(@PathVariable UUID itemId) {
        items.eliminar(itemId);
    }
}
