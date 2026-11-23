package com.regenta.compras.web;

import com.regenta.compras.api.*;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.GestionDeSugerencias;
import com.regenta.compras.application.GrupoDeSugerencias;
import com.regenta.compras.application.OrdenDelNegocio;
import com.regenta.compras.application.SolicitudDeAceptacion;
import com.regenta.compras.application.SolicitudDeProveedorParaSugerencia;
import com.regenta.compras.application.SugerenciaDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Sugerencia de compra a partir de stock bajo mínimo. HU-050, HU-181. */
@RestController
@RequestMapping("/api/compras/sugerencias")
@Tag(name = "Sugerencias de compra", description = "Qué reponer, agrupado por proveedor preferido")
public class SugerenciasDeCompraControlador {

    private final GestionDeSugerencias sugerencias;

    public SugerenciasDeCompraControlador(GestionDeSugerencias sugerencias) {
        this.sugerencias = sugerencias;
    }

    @GetMapping
    @Operation(summary = "Las sugerencias pendientes, agrupadas por proveedor preferido y bodega",
            description = "Una por producto y bodega, con la cantidad hasta el stock objetivo redondeada al "
                    + "múltiplo que vende el proveedor (I30). Las de productos sin proveedor van en un grupo "
                    + "aparte, al final, con sinProveedor = true (I31).")
    public List<GrupoDeSugerencias> listar() {
        return sugerencias.listar();
    }

    @PostMapping("/aceptacion")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Acepta el grupo de un proveedor: crea una orden de compra en borrador")
    @ApiResponse(responseCode = "422", description = "Sugerencias sin proveedor, sin bodega destino o de "
            + "varias bodegas sin indicar cuál")
    public OrdenDelNegocio aceptar(@Valid @RequestBody SolicitudDeAceptacion solicitud) {
        return sugerencias.aceptar(solicitud);
    }

    @PutMapping("/{sugerenciaId}/proveedor")
    @Operation(summary = "Le asigna un proveedor a un producto que no tenía (HU-181 C5)",
            description = "El proveedor queda como preferido del producto en su catálogo, y las sugerencias "
                    + "pendientes del producto pasan a su grupo con la cantidad redondeada a su mínimo.")
    @ApiResponse(responseCode = "403", description = "Sin COMPRAS_COMPRA_CREAR o sin COMPRAS_PROVEEDOR_EDITAR")
    @ApiResponse(responseCode = "404", description = "La sugerencia o el proveedor no existen")
    @ApiResponse(responseCode = "409", description = "El proveedor está desactivado")
    @ApiResponse(responseCode = "422", description = "La sugerencia ya no está pendiente")
    public SugerenciaDelNegocio asignarProveedor(@PathVariable UUID sugerenciaId,
            @Valid @RequestBody SolicitudDeProveedorParaSugerencia solicitud) {
        return sugerencias.asignarProveedor(sugerenciaId, solicitud);
    }

    @PostMapping("/{sugerenciaId}/descarte")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Descarta una sugerencia")
    public void descartar(@PathVariable UUID sugerenciaId) {
        sugerencias.descartar(sugerenciaId);
    }
}
