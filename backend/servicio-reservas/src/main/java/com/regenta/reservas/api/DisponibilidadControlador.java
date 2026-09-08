package com.regenta.reservas.api;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.reservas.aplicacion.ConsultaDeDisponibilidad;
import com.regenta.reservas.aplicacion.DisponibilidadEnPeriodo;
import com.regenta.reservas.aplicacion.SolicitudDeDisponibilidad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Consulta de disponibilidad de recursos en un periodo. HU-069. */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Disponibilidad", description = "Qué recursos hay libres entre dos fechas")
public class DisponibilidadControlador {

    private final ConsultaDeDisponibilidad disponibilidad;

    public DisponibilidadControlador(ConsultaDeDisponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @GetMapping("/disponibilidad")
    @Operation(summary = "Recursos libres entre desde y hasta, descontando reservas y bloqueos")
    public DisponibilidadEnPeriodo consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) UUID tipoRecursoId,
            @RequestParam(required = false) Integer personas) {
        return disponibilidad.consultar(
                new SolicitudDeDisponibilidad(desde, hasta, tipoRecursoId, personas));
    }
}
