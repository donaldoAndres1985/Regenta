package com.regenta.caja.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.caja.aplicacion.ReporteDeCaja;
import com.regenta.caja.aplicacion.ReporteDeSesion;
import com.regenta.caja.aplicacion.SesionEnReporte;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Reporte de cierre de caja para el gerente. HU-063. */
@RestController
@RequestMapping("/api/caja/reportes")
@Tag(name = "Reportes de caja", description = "Resumen de los turnos y sus descuadres")
public class ReportesDeCajaControlador {

    private final ReporteDeCaja reportes;

    public ReportesDeCajaControlador(ReporteDeCaja reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/sesiones/{sesionId}")
    @Operation(summary = "El resumen de un turno: totales por método, movimientos y diferencia")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ReporteDeSesion sesion(@PathVariable UUID sesionId) {
        return reportes.deSesion(sesionId);
    }

    @GetMapping("/sesiones")
    @Operation(summary = "Las sesiones de un rango de fechas, con su estado")
    public List<SesionEnReporte> sesiones(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) UUID cajaId) {
        return reportes.sesiones(desde, hasta, estado, cajaId);
    }
}
