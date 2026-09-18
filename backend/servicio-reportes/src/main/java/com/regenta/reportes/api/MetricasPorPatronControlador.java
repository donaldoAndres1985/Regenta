package com.regenta.reportes.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.reportes.aplicacion.CancelacionesDelPeriodo;
import com.regenta.reportes.aplicacion.MesasDelPeriodo;
import com.regenta.reportes.aplicacion.MetricasDeComanda;
import com.regenta.reportes.aplicacion.MetricasDeReserva;
import com.regenta.reportes.aplicacion.OcupacionDelDia;
import com.regenta.reportes.aplicacion.PreparacionDePlato;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Métricas propias de cada patrón operativo. HU-099. */
@RestController
@RequestMapping("/api/reportes/metricas")
@Tag(name = "Métricas por patrón",
        description = "Ocupación, ADR y RevPAR en Reserva; mesas y cocina en Comanda")
public class MetricasPorPatronControlador {

    private final MetricasDeReserva reserva;
    private final MetricasDeComanda comanda;

    public MetricasPorPatronControlador(MetricasDeReserva reserva, MetricasDeComanda comanda) {
        this.reserva = reserva;
        this.comanda = comanda;
    }

    /**
     * Criterio 3: qué puede pedir este negocio. El dashboard pregunta esto
     * primero y dibuja solo lo que le responda — así no tiene que conocer la
     * tabla de patrones ni adivinar qué endpoint le va a dar 404.
     */
    @GetMapping
    @Operation(summary = "Las métricas que aplican al patrón operativo de este negocio")
    public List<String> disponibles() {
        String patron = ContextoDeNegocio.actual().patron();
        if (patron == null) {
            return List.of();
        }
        return switch (patron.toUpperCase(java.util.Locale.ROOT)) {
            case "RESERVA" -> List.of("ocupacion", "cancelaciones");
            case "COMANDA" -> List.of("mesas", "preparacion");
            default -> List.of();
        };
    }

    @GetMapping("/ocupacion")
    @Operation(summary = "Ocupación, ADR y RevPAR por día y tipo de recurso")
    @ApiResponse(responseCode = "404", description = "El negocio no es del patrón Reserva")
    public List<OcupacionDelDia> ocupacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reserva.ocupacion(desde, hasta);
    }

    @GetMapping("/cancelaciones")
    @Operation(summary = "Tasa de cancelación y de no-show, y la penalización cobrada")
    @ApiResponse(responseCode = "404", description = "El negocio no es del patrón Reserva")
    public CancelacionesDelPeriodo cancelaciones(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reserva.cancelaciones(desde, hasta);
    }

    @GetMapping("/mesas")
    @Operation(summary = "Rotación de mesas, tiempo medio de mesa y ticket por comensal")
    @ApiResponse(responseCode = "404", description = "El negocio no es del patrón Comanda")
    public MesasDelPeriodo mesas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return comanda.mesas(desde, hasta);
    }

    @GetMapping("/preparacion")
    @Operation(summary = "Tiempo medio de preparación de cada plato, según las marcas del KDS")
    @ApiResponse(responseCode = "404", description = "El negocio no es del patrón Comanda")
    public List<PreparacionDePlato> preparacion(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return comanda.preparacion(desde, hasta);
    }
}
