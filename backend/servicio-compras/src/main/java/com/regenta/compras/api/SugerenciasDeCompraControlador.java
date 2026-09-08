package com.regenta.compras.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.aplicacion.GestionDeSugerencias;
import com.regenta.compras.aplicacion.GrupoDeSugerencias;
import com.regenta.compras.aplicacion.OrdenDelNegocio;
import com.regenta.compras.aplicacion.SolicitudDeAceptacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Sugerencia de compra a partir de stock bajo mínimo. HU-050. */
@RestController
@RequestMapping("/api/compras/sugerencias")
@Tag(name = "Sugerencias de compra", description = "Qué reponer, agrupado por proveedor preferido")
public class SugerenciasDeCompraControlador {

    private final GestionDeSugerencias sugerencias;

    public SugerenciasDeCompraControlador(GestionDeSugerencias sugerencias) {
        this.sugerencias = sugerencias;
    }

    @GetMapping
    @Operation(summary = "Las sugerencias pendientes, agrupadas por proveedor preferido")
    public List<GrupoDeSugerencias> listar() {
        return sugerencias.listar();
    }

    @PostMapping("/aceptacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Acepta el grupo de un proveedor: crea una orden de compra en borrador")
    @ApiResponse(responseCode = "422", description = "Sugerencias sin proveedor o sin bodega destino")
    public OrdenDelNegocio aceptar(@Valid @RequestBody SolicitudDeAceptacion solicitud) {
        return sugerencias.aceptar(solicitud);
    }

    @PostMapping("/{sugerenciaId}/descarte")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Descarta una sugerencia")
    public void descartar(@PathVariable UUID sugerenciaId) {
        sugerencias.descartar(sugerenciaId);
    }
}
