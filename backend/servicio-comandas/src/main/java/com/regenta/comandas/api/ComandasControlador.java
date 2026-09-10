package com.regenta.comandas.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comandas.aplicacion.ComandaDetallada;
import com.regenta.comandas.aplicacion.GestionDeComandas;
import com.regenta.comandas.aplicacion.SolicitudDeAjusteDeLinea;
import com.regenta.comandas.aplicacion.SolicitudDeAperturaComanda;
import com.regenta.comandas.aplicacion.SolicitudDeLinea;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Comandas: abrir y agregar líneas mientras el servicio avanza. HU-085. */
@RestController
@RequestMapping("/api/comandas")
@Tag(name = "Comandas", description = "El pedido que queda abierto y acumula líneas por rondas")
public class ComandasControlador {

    private final GestionDeComandas comandas;

    public ComandasControlador(GestionDeComandas comandas) {
        this.comandas = comandas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre una comanda sobre una sesión de mesa")
    @ApiResponse(responseCode = "409", description = "La sesión ya tiene una comanda abierta")
    public ComandaDetallada abrir(@Valid @RequestBody SolicitudDeAperturaComanda solicitud) {
        return comandas.abrir(solicitud);
    }

    @GetMapping("/{comandaId}")
    @Operation(summary = "Una comanda con sus líneas, cada una con su estado")
    public ComandaDetallada ver(@PathVariable UUID comandaId) {
        return comandas.ver(comandaId);
    }

    @GetMapping
    @Operation(summary = "La comanda abierta de una sesión de mesa")
    public ComandaDetallada deSesion(@RequestParam UUID sesionMesaId) {
        return comandas.comandaDeSesion(sesionMesaId);
    }

    @PostMapping("/{comandaId}/lineas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agrega una línea; nace PENDIENTE aunque la comanda ya esté en cocina")
    @ApiResponse(responseCode = "422", description = "Faltan modificadores obligatorios del ítem")
    public ComandaDetallada agregarLinea(@PathVariable UUID comandaId,
            @Valid @RequestBody SolicitudDeLinea solicitud) {
        return comandas.agregarLinea(comandaId, solicitud);
    }

    @PostMapping("/{comandaId}/envio")
    @Operation(summary = "Envía a cocina las líneas pendientes")
    public ComandaDetallada enviarACocina(@PathVariable UUID comandaId) {
        return comandas.enviarACocina(comandaId);
    }

    @PostMapping("/{comandaId}/lineas/{lineaId}/avance")
    @Operation(summary = "Avanza el estado de la línea (PENDIENTE → ENVIADA → … → ENTREGADA)")
    public ComandaDetallada avanzarLinea(@PathVariable UUID comandaId,
            @PathVariable UUID lineaId) {
        return comandas.avanzarLinea(comandaId, lineaId);
    }

    @PutMapping("/{comandaId}/lineas/{lineaId}")
    @Operation(summary = "Ajusta el curso y la secuencia de envío de una línea pendiente")
    public ComandaDetallada ajustarLinea(@PathVariable UUID comandaId, @PathVariable UUID lineaId,
            @RequestBody SolicitudDeAjusteDeLinea solicitud) {
        return comandas.ajustarLinea(comandaId, lineaId, solicitud);
    }
}
