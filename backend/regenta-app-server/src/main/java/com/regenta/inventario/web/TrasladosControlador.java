package com.regenta.inventario.web;

import com.regenta.inventario.api.*;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.application.GestionDeTraslados;
import com.regenta.inventario.application.SolicitudDeRecepcion;
import com.regenta.inventario.application.SolicitudDeTraslado;
import com.regenta.inventario.application.TrasladoDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Traslados de mercancÃ­a entre bodegas. HU-032. */
@RestController
@RequestMapping("/api/inventario/traslados")
@Tag(name = "Traslados", description = "Mover mercancÃ­a de una bodega a otra sin perderla en el camino")
public class TrasladosControlador {

    private final GestionDeTraslados traslados;

    public TrasladosControlador(GestionDeTraslados traslados) {
        this.traslados = traslados;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un traslado en BORRADOR con sus lÃ­neas")
    @ApiResponse(responseCode = "409", description = "Ya hay un traslado con ese nÃºmero")
    @ApiResponse(responseCode = "422", description = "Bodega origen igual a destino, o falta el lote")
    public TrasladoDelNegocio crear(@Valid @RequestBody SolicitudDeTraslado solicitud) {
        return traslados.crear(solicitud);
    }

    @PostMapping("/{trasladoId}/envio")
    @Operation(summary = "EnvÃ­a el traslado: la mercancÃ­a sale de origen y entra a la bodega de trÃ¡nsito")
    @ApiResponse(responseCode = "409", description = "El traslado no estÃ¡ en BORRADOR")
    public TrasladoDelNegocio enviar(@PathVariable UUID trasladoId) {
        return traslados.enviar(trasladoId);
    }

    @PostMapping("/{trasladoId}/recepcion")
    @Operation(summary = "Recibe el traslado: sale de trÃ¡nsito y entra a destino")
    @ApiResponse(responseCode = "409", description = "El traslado no estÃ¡ EN_TRANSITO")
    @ApiResponse(responseCode = "422", description = "Se intentÃ³ recibir mÃ¡s de lo enviado")
    public TrasladoDelNegocio recibir(@PathVariable UUID trasladoId,
            @Valid @RequestBody SolicitudDeRecepcion solicitud) {
        return traslados.recibir(trasladoId, solicitud);
    }

    @GetMapping("/{trasladoId}")
    @Operation(summary = "Un traslado con sus lÃ­neas")
    public TrasladoDelNegocio ver(@PathVariable UUID trasladoId) {
        return traslados.ver(trasladoId);
    }
}
