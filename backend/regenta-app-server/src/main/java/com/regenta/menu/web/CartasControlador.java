package com.regenta.menu.web;

import com.regenta.menu.api.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.regenta.menu.application.CartaConCategorias;
import com.regenta.menu.application.CartaDeMenu;
import com.regenta.menu.application.CartaDelNegocio;
import com.regenta.menu.application.CategoriaDelNegocio;
import com.regenta.menu.application.GestionDeCartas;
import com.regenta.menu.application.GestionDeItemsDeMenu;
import com.regenta.menu.application.SolicitudDeCarta;
import com.regenta.menu.application.SolicitudDeCategoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Cartas y categorÃ­as del menÃº. HU-076. */
@RestController
@RequestMapping("/api/menu/cartas")
@Tag(name = "Cartas", description = "Cartas por franja horaria y sus categorÃ­as")
public class CartasControlador {

    private final GestionDeCartas cartas;
    private final GestionDeItemsDeMenu items;

    public CartasControlador(GestionDeCartas cartas, GestionDeItemsDeMenu items) {
        this.cartas = cartas;
        this.items = items;
    }

    @GetMapping
    @Operation(summary = "Todas las cartas del negocio, la de por defecto primero")
    public List<CartaDelNegocio> listar() {
        return cartas.listar();
    }

    @GetMapping("/disponibles")
    @Operation(summary = "Las cartas activas que valen ahora (o en el instante indicado)")
    public List<CartaDelNegocio> disponibles(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime momento) {
        return cartas.disponibles(momento);
    }

    @GetMapping("/{cartaId}")
    @Operation(summary = "Una carta con sus categorÃ­as en orden")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public CartaConCategorias ver(@PathVariable UUID cartaId) {
        return cartas.ver(cartaId);
    }

    @GetMapping("/{cartaId}/menu")
    @Operation(summary = "La carta como la ve el mesero: categorÃ­as con sus Ã­tems (agotados incluidos)")
    public CartaDeMenu menu(@PathVariable UUID cartaId) {
        return items.menuDeCarta(cartaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una carta")
    public CartaDelNegocio crear(@Valid @RequestBody SolicitudDeCarta solicitud) {
        return cartas.crear(solicitud);
    }

    @PutMapping("/{cartaId}")
    @Operation(summary = "Cambia una carta")
    public CartaDelNegocio actualizar(@PathVariable UUID cartaId,
            @Valid @RequestBody SolicitudDeCarta solicitud) {
        return cartas.actualizar(cartaId, solicitud);
    }

    @PostMapping("/{cartaId}/activacion")
    @Operation(summary = "Activa o desactiva la carta")
    public CartaDelNegocio activacion(@PathVariable UUID cartaId,
            @RequestBody Map<String, Boolean> cuerpo) {
        return cartas.cambiarActivacion(cartaId, Boolean.TRUE.equals(cuerpo.get("activa")));
    }

    @PostMapping("/{cartaId}/por-defecto")
    @Operation(summary = "Marca esta carta como la de por defecto")
    public CartaDelNegocio porDefecto(@PathVariable UUID cartaId) {
        return cartas.marcarPorDefecto(cartaId);
    }

    @DeleteMapping("/{cartaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una carta (no si tiene platos)")
    @ApiResponse(responseCode = "409", description = "La carta tiene platos")
    public void eliminar(@PathVariable UUID cartaId) {
        cartas.eliminar(cartaId);
    }

    @PostMapping("/{cartaId}/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una categorÃ­a a la carta")
    public CategoriaDelNegocio crearCategoria(@PathVariable UUID cartaId,
            @Valid @RequestBody SolicitudDeCategoria solicitud) {
        return cartas.crearCategoria(cartaId, solicitud);
    }

    @PutMapping("/categorias/{categoriaId}")
    @Operation(summary = "Cambia una categorÃ­a")
    public CategoriaDelNegocio actualizarCategoria(@PathVariable UUID categoriaId,
            @Valid @RequestBody SolicitudDeCategoria solicitud) {
        return cartas.actualizarCategoria(categoriaId, solicitud);
    }

    @DeleteMapping("/categorias/{categoriaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Quita una categorÃ­a de la carta")
    public void quitarCategoria(@PathVariable UUID categoriaId) {
        cartas.quitarCategoria(categoriaId);
    }

    @PutMapping("/{cartaId}/orden-categorias")
    @Operation(summary = "Fija el orden de las categorÃ­as segÃºn la lista de ids")
    public List<CategoriaDelNegocio> reordenar(@PathVariable UUID cartaId,
            @RequestBody List<UUID> ordenIds) {
        return cartas.reordenarCategorias(cartaId, ordenIds);
    }
}
