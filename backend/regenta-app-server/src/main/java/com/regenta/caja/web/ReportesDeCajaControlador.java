package com.regenta.caja.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.caja.application.ConsultaDeCierres;
import com.regenta.caja.application.ConsultaDeCierres.Cajero;
import com.regenta.caja.application.ConsultaDeCierres.DetalleDeMovimiento;
import com.regenta.caja.application.ConsultaDeCierres.MovimientosDeCierre;
import com.regenta.caja.application.ConsultaDeCierres.PaginaDeCierres;
import com.regenta.caja.application.ConsultaDeCierres.ResumenDeCierre;
import com.regenta.caja.application.InformesDeCajaEnPdf;
import com.regenta.caja.application.InformesDeCajaEnPdf.InformeEnPdf;
import com.regenta.caja.application.ReporteDeCaja;
import com.regenta.caja.application.ReporteDeSesion;
import com.regenta.caja.application.SesionEnReporte;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Reporte de cierre de caja para el gerente (HU-063) y consulta de cierres pasados (HU-143). */
@RestController
@RequestMapping("/api/caja/reportes")
@Tag(name = "Reportes de caja", description = "Resumen de los turnos y sus descuadres")
public class ReportesDeCajaControlador {

    private final ReporteDeCaja reportes;
    private final ConsultaDeCierres cierres;
    private final InformesDeCajaEnPdf informes;

    public ReportesDeCajaControlador(ReporteDeCaja reportes, ConsultaDeCierres cierres,
            InformesDeCajaEnPdf informes) {
        this.reportes = reportes;
        this.cierres = cierres;
        this.informes = informes;
    }

    @GetMapping(value = "/cierres/{sesionId}/pdf", produces = "application/pdf")
    @Operation(summary = "El informe de un cierre en PDF (HU-144)",
            description = "tipo=general: el resumen y el arqueo por método. tipo=detallado: además todos los "
                    + "movimientos con el total.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso CAJA_TURNO_VER")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    @ApiResponse(responseCode = "409", description = "La sesión sigue abierta")
    @ApiResponse(responseCode = "422", description = "Tipo de informe desconocido")
    public org.springframework.http.ResponseEntity<byte[]> pdfDeCierre(@PathVariable UUID sesionId,
            @RequestParam(defaultValue = "general") String tipo) {
        boolean detallado = switch (tipo.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "general" -> false;
            case "detallado" -> true;
            default -> throw new com.regenta.shared.web.ReglaDeNegocioException(
                    "El informe de un cierre es general o detallado");
        };
        return pdf(informes.deCierre(sesionId, detallado));
    }

    @GetMapping(value = "/cierres/pdf", produces = "application/pdf")
    @Operation(summary = "Los cierres de un rango en PDF, uno por fila, con los totales (HU-144)")
    @ApiResponse(responseCode = "403", description = "Sin el permiso CAJA_TURNO_VER")
    @ApiResponse(responseCode = "422", description = "Rango de fechas no válido")
    public org.springframework.http.ResponseEntity<byte[]> pdfDeCierres(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID cajeroId) {
        return pdf(informes.deCierres(desde, hasta, cajeroId));
    }

    private static org.springframework.http.ResponseEntity<byte[]> pdf(InformeEnPdf informe) {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + informe.nombre() + "\"")
                .body(informe.contenido());
    }

    @GetMapping("/cierres")
    @Operation(summary = "Busca los cierres de caja de un rango, opcionalmente de un cajero (HU-143)",
            description = "Por fecha de cierre, el más reciente primero, paginados (página desde 0, de 1 a 100 "
                    + "cierres; 20 por defecto). Cada uno con fecha, caja, número, cajero, total esperado, "
                    + "diferencia total y estado. El cajero es el que abrió o el que cerró la sesión.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso CAJA_TURNO_VER")
    @ApiResponse(responseCode = "422", description = "Rango de fechas o paginación no válidos")
    public PaginaDeCierres buscarCierres(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID cajeroId,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano) {
        return cierres.buscar(desde, hasta, cajeroId, pagina, tamano);
    }

    @GetMapping("/cajeros")
    @Operation(summary = "Quienes han abierto caja en el negocio, para filtrar los cierres (HU-143)")
    @ApiResponse(responseCode = "403", description = "Sin el permiso CAJA_TURNO_VER")
    public List<Cajero> cajeros() {
        return cierres.cajeros();
    }

    @GetMapping("/cierres/{sesionId}")
    @Operation(summary = "El resumen de un cierre (HU-143)",
            description = "Ventas al contado y a crédito, devoluciones, recaudos, ingresos, retiros, gastos y "
                    + "pagos a proveedor, y el arqueo por método con esperado, contado y diferencia. Un "
                    + "movimiento anulado no suma.")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    @ApiResponse(responseCode = "409", description = "La sesión sigue abierta")
    public ResumenDeCierre resumenDeCierre(@PathVariable UUID sesionId) {
        return cierres.resumen(sesionId);
    }

    @GetMapping("/cierres/{sesionId}/movimientos")
    @Operation(summary = "Los movimientos de un cierre con el total (HU-143)",
            description = "Fecha, tipo con su nombre, documento, método y monto con su signo.")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    @ApiResponse(responseCode = "409", description = "La sesión sigue abierta")
    public MovimientosDeCierre movimientosDeCierre(@PathVariable UUID sesionId) {
        return cierres.movimientosDe(sesionId);
    }

    @GetMapping("/movimientos/{movimientoId}")
    @Operation(summary = "El detalle de un movimiento de caja (HU-143)",
            description = "Quién lo registró, quién lo autorizó o anuló, la venta de la que viene (ventaId) y, "
                    + "si fue un cobro mixto, todos sus métodos en pagos.")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public DetalleDeMovimiento movimiento(@PathVariable UUID movimientoId) {
        return cierres.movimiento(movimientoId);
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
