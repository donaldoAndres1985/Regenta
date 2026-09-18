package com.regenta.ventas.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.aplicacion.GestionDeVentasOffline;
import com.regenta.ventas.aplicacion.SolicitudDeVentaOffline;
import com.regenta.ventas.aplicacion.VentaDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Subida de las ventas registradas sin señal. HU-043. */
@RestController
@RequestMapping("/api/ventas/offline")
@Tag(name = "Ventas offline", description = "Sube las ventas que se registraron sin conexión")
public class VentasOfflineControlador {

    private final GestionDeVentasOffline offline;

    public VentasOfflineControlador(GestionDeVentasOffline offline) {
        this.offline = offline;
    }

    @PostMapping
    @Operation(summary = "Sube una venta hecha sin señal, completa y confirmándose; "
            + "reintentar con el mismo origenOfflineId devuelve la ya creada, no una segunda")
    public VentaDelNegocio subir(@Valid @RequestBody SolicitudDeVentaOffline solicitud) {
        return offline.subir(solicitud);
    }

    @GetMapping("/{origenOfflineId}")
    @Operation(summary = "La venta que subió con ese id del celular")
    @ApiResponse(responseCode = "404", description = "Esa venta no se ha subido todavía")
    public VentaDelNegocio ver(@PathVariable UUID origenOfflineId) {
        return offline.verPorIdOffline(origenOfflineId);
    }
}
