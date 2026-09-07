package com.regenta.inventario.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.aplicacion.GestionDeReservasStock;
import com.regenta.inventario.aplicacion.ResultadoDeReserva;
import com.regenta.inventario.aplicacion.SolicitudDeReservaStock;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Reservas de stock para la saga de ventas. HU-034.
 *
 * <p>Normalmente lo maneja el consumidor de eventos ({@code solicitar_reserva_stock},
 * {@code venta_completada}); estos endpoints existen para la integración de la saga
 * y para operar a mano.
 */
@RestController
@RequestMapping("/api/inventario/reservas")
@Tag(name = "Reservas de stock", description = "Apartar y liberar stock para la saga de ventas")
public class ReservasStockControlador {

    private final GestionDeReservasStock reservas;

    public ReservasStockControlador(GestionDeReservasStock reservas) {
        this.reservas = reservas;
    }

    @PostMapping
    @Operation(summary = "Solicita apartar stock; publica stock_reservado o stock_reserva_fallida")
    public ResultadoDeReserva solicitar(@Valid @RequestBody SolicitudDeReservaStock solicitud) {
        return reservas.solicitar(solicitud);
    }

    @PostMapping("/{origenTipo}/{origenId}/confirmacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirma la reserva de una venta: la convierte en salida real")
    public void confirmar(@PathVariable String origenTipo, @PathVariable UUID origenId) {
        reservas.confirmar(origenTipo, origenId);
    }

    @PostMapping("/barrido-vencidas")
    @Operation(summary = "Libera las reservas de este negocio que ya vencieron")
    public int barrerVencidas() {
        return reservas.liberarExpiradas();
    }
}
