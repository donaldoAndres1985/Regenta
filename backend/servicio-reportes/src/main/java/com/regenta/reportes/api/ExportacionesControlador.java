package com.regenta.reportes.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.reportes.aplicacion.ArchivoDeReporte;
import com.regenta.reportes.aplicacion.CatalogoDeReportes;
import com.regenta.reportes.aplicacion.EjecucionDeReporte;
import com.regenta.reportes.aplicacion.GestionDeExportaciones;
import com.regenta.reportes.aplicacion.GestionDeProgramaciones;
import com.regenta.reportes.aplicacion.ReporteProgramado;
import com.regenta.reportes.aplicacion.SolicitudDeExportacion;
import com.regenta.reportes.aplicacion.SolicitudDeProgramacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Exportar un reporte y programar que llegue solo. HU-100. */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Exportaciones", description = "Exportar a PDF, XLSX o CSV y programar envíos")
public class ExportacionesControlador {

    private final GestionDeExportaciones exportaciones;
    private final GestionDeProgramaciones programaciones;
    private final CatalogoDeReportes catalogo;

    public ExportacionesControlador(GestionDeExportaciones exportaciones,
            GestionDeProgramaciones programaciones, CatalogoDeReportes catalogo) {
        this.exportaciones = exportaciones;
        this.programaciones = programaciones;
        this.catalogo = catalogo;
    }

    @GetMapping("/exportables")
    @Operation(summary = "Qué reportes se pueden exportar en este negocio, según su patrón")
    public List<Map<String, String>> exportables() {
        return catalogo.delPatron(ContextoDeNegocio.actual().patron()).stream()
                .map(r -> Map.of("codigo", r.codigo(), "nombre", r.nombre()))
                .toList();
    }

    @PostMapping("/exportaciones")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Pide la exportación y devuelve el comprobante; el archivo se hace aparte")
    @ApiResponse(responseCode = "202", description = "Encolada: consultar la ejecución hasta COMPLETADO")
    @ApiResponse(responseCode = "404", description = "No hay un reporte con ese código")
    public EjecucionDeReporte exportar(@Valid @RequestBody SolicitudDeExportacion solicitud) {
        return exportaciones.solicitar(solicitud);
    }

    @PostMapping("/exportaciones/barrido")
    @Operation(summary = "Genera lo que este negocio tenga pendiente, y reintenta lo que falló")
    public Map<String, Integer> barrerExportaciones() {
        return Map.of("generadas", exportaciones.procesarPendientes());
    }

    @GetMapping("/ejecuciones")
    @Operation(summary = "Las últimas exportaciones del negocio, con su estado y su error")
    public List<EjecucionDeReporte> historial() {
        return exportaciones.historial();
    }

    @GetMapping("/ejecuciones/{ejecucionId}")
    @Operation(summary = "Una exportación: si ya está COMPLETADO, el archivo se puede descargar")
    public EjecucionDeReporte ver(@PathVariable UUID ejecucionId) {
        return exportaciones.ver(ejecucionId);
    }

    @GetMapping("/ejecuciones/{ejecucionId}/archivo")
    @Operation(summary = "Descarga el archivo generado")
    @ApiResponse(responseCode = "404", description = "Esa exportación todavía no tiene archivo")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID ejecucionId) {
        ArchivoDeReporte archivo = exportaciones.archivo(ejecucionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre()).build().toString())
                .contentType(MediaType.parseMediaType(archivo.tipoMime()))
                .body(archivo.contenido());
    }

    @PostMapping("/programaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Programa un reporte con un cron y sus destinatarios")
    @ApiResponse(responseCode = "422", description = "El cron no se entiende o el formato no existe")
    public ReporteProgramado programar(@Valid @RequestBody SolicitudDeProgramacion solicitud) {
        return programaciones.programar(solicitud);
    }

    @GetMapping("/programaciones")
    @Operation(summary = "Las programaciones del negocio")
    public List<ReporteProgramado> programaciones() {
        return programaciones.listar();
    }

    @PostMapping("/programaciones/barrido")
    @Operation(summary = "Encola las programaciones vencidas y las vuelve a agendar")
    public Map<String, Integer> barrerProgramaciones() {
        return Map.of("encoladas", programaciones.barrer());
    }
}
