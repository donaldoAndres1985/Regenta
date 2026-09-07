package com.regenta.ventas.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.aplicacion.DevolucionDelNegocio;
import com.regenta.ventas.aplicacion.GestionDeDevoluciones;
import com.regenta.ventas.aplicacion.SolicitudDeDevolucion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Devoluciones de una venta. HU-042. */
@RestController
@RequestMapping("/api/ventas/{ventaId}/devoluciones")
@Tag(name = "Devoluciones", description = "Recibir la devolución de parte o de toda una venta")
public class DevolucionesControlador {

    private final GestionDeDevoluciones devoluciones;

    public DevolucionesControlador(GestionDeDevoluciones devoluciones) {
        this.devoluciones = devoluciones;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra una devolución; publica devolucion_registrada")
    @ApiResponse(responseCode = "409", description = "La venta no admite devoluciones")
    @ApiResponse(responseCode = "422", description = "Se intentó devolver más de lo vendido")
    public DevolucionDelNegocio registrar(@PathVariable UUID ventaId,
            @Valid @RequestBody SolicitudDeDevolucion solicitud) {
        return devoluciones.registrar(ventaId, solicitud);
    }
}
