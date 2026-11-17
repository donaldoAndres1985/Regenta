package com.regenta.inventario.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.application.GestionDeProductos;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Unidades de medida del negocio, para elegir en la ficha del producto (HU-150). */
@RestController
@RequestMapping("/api/inventario/unidades")
@Tag(name = "Unidades", description = "Unidades de medida del negocio")
public class UnidadesControlador {

    private final GestionDeProductos productos;

    public UnidadesControlador(GestionDeProductos productos) {
        this.productos = productos;
    }

    @GetMapping
    @Operation(summary = "Las unidades de medida del negocio")
    public List<Unidad> listar() {
        return productos.unidades().stream()
                .map(u -> new Unidad(u.getId(), u.getCodigo(), u.getNombre()))
                .toList();
    }

    public record Unidad(UUID id, String codigo, String nombre) {
    }
}
