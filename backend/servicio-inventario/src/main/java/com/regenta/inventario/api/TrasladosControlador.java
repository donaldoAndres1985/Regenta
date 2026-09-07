package com.regenta.inventario.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.aplicacion.GestionDeTraslados;
import com.regenta.inventario.aplicacion.SolicitudDeRecepcion;
import com.regenta.inventario.aplicacion.SolicitudDeTraslado;
import com.regenta.inventario.aplicacion.TrasladoDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Traslados de mercancía entre bodegas. HU-032. */
@RestController
@RequestMapping("/api/inventario/traslados")
@Tag(name = "Traslados", description = "Mover mercancía de una bodega a otra sin perderla en el camino")
public class TrasladosControlador {

    private final GestionDeTraslados traslados;

    public TrasladosControlador(GestionDeTraslados traslados) {
        this.traslados = traslados;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un traslado en BORRADOR con sus líneas")
    @ApiResponse(responseCode = "409", description = "Ya hay un traslado con ese número")
    @ApiResponse(responseCode = "422", description = "Bodega origen igual a destino, o falta el lote")
    public TrasladoDelNegocio crear(@Valid @RequestBody SolicitudDeTraslado solicitud) {
        return traslados.crear(solicitud);
    }

    @PostMapping("/{trasladoId}/envio")
    @Operation(summary = "Envía el traslado: la mercancía sale de origen y entra a la bodega de tránsito")
    @ApiResponse(responseCode = "409", description = "El traslado no está en BORRADOR")
    public TrasladoDelNegocio enviar(@PathVariable UUID trasladoId) {
        return traslados.enviar(trasladoId);
    }

    @PostMapping("/{trasladoId}/recepcion")
    @Operation(summary = "Recibe el traslado: sale de tránsito y entra a destino")
    @ApiResponse(responseCode = "409", description = "El traslado no está EN_TRANSITO")
    @ApiResponse(responseCode = "422", description = "Se intentó recibir más de lo enviado")
    public TrasladoDelNegocio recibir(@PathVariable UUID trasladoId,
            @Valid @RequestBody SolicitudDeRecepcion solicitud) {
        return traslados.recibir(trasladoId, solicitud);
    }

    @GetMapping("/{trasladoId}")
    @Operation(summary = "Un traslado con sus líneas")
    public TrasladoDelNegocio ver(@PathVariable UUID trasladoId) {
        return traslados.ver(trasladoId);
    }
}
