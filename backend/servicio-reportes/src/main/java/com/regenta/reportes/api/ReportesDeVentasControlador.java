package com.regenta.reportes.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.reportes.aplicacion.ReporteDeVentas;
import com.regenta.reportes.aplicacion.RotacionProducto;
import com.regenta.reportes.aplicacion.VentaPorCategoria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Reportes de ventas, márgenes y rotación. HU-098. */
@RestController
@RequestMapping("/api/reportes/ventas")
@Tag(name = "Reportes de ventas", description = "Ventas por categoría, márgenes al costo de cada venta y rotación")
public class ReportesDeVentasControlador {

    private final ReporteDeVentas reporte;

    public ReportesDeVentasControlador(ReporteDeVentas reporte) {
        this.reporte = reporte;
    }

    @GetMapping("/por-categoria")
    @Operation(summary = "Monto, unidades y margen por categoría en un rango de fechas")
    public List<VentaPorCategoria> porCategoria(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) {
        return reporte.porCategoria(desde, hasta, sucursalId);
    }

    @GetMapping("/rotacion")
    @Operation(summary = "Días sin movimiento de inventario de cada producto vigente")
    public List<RotacionProducto> rotacion(@RequestParam(required = false) UUID sucursalId) {
        return reporte.rotacion(sucursalId);
    }
}
