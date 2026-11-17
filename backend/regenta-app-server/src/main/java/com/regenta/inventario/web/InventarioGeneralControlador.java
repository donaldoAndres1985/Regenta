package com.regenta.inventario.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.application.FiltroDeInventario;
import com.regenta.inventario.application.InventarioDelNegocio;
import com.regenta.inventario.application.InventarioGeneral;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-153 · el inventario general del negocio. */
@RestController
@RequestMapping("/api/inventario/general")
@Tag(name = "Inventario general", description = "Productos con existencia y valor, filtrados")
public class InventarioGeneralControlador {

    private final InventarioGeneral inventario;

    public InventarioGeneralControlador(InventarioGeneral inventario) {
        this.inventario = inventario;
    }

    @GetMapping
    @Operation(summary = "Productos con existencia, costo, precio y valor; totales de todo lo filtrado")
    public InventarioDelNegocio consultar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false, defaultValue = "ACTIVOS") String estado,
            @RequestParam(defaultValue = "false") boolean tarifaPorRevisar,
            @RequestParam(defaultValue = "false") boolean existenciaNegativa,
            @RequestParam(required = false) UUID bodegaId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {
        return inventario.consultar(new FiltroDeInventario(q, categoriaId, estado, tarifaPorRevisar,
                existenciaNegativa, bodegaId, pagina, tamano));
    }
}
