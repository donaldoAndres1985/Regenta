package com.regenta.inventario.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.regenta.inventario.application.FilaImportada;
import com.regenta.inventario.application.ImportacionDeProductos;
import com.regenta.inventario.application.ResultadoDeImportacion;
import com.regenta.inventario.application.VistaPreviaDeImportacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-164 · importar productos desde Excel. */
@RestController
@RequestMapping("/api/inventario/importacion")
@Tag(name = "Importacion", description = "Cargar productos desde un .xlsx")
public class ImportacionControlador {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ImportacionDeProductos importacion;

    public ImportacionControlador(ImportacionDeProductos importacion) {
        this.importacion = importacion;
    }

    @GetMapping("/plantilla")
    @Operation(summary = "La plantilla .xlsx con las columnas que se importan")
    public ResponseEntity<byte[]> plantilla() {
        return archivo(importacion.plantilla(), "plantilla-productos.xlsx");
    }

    @PostMapping(value = "/vista-previa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cada fila con sus errores, sin grabar nada")
    public VistaPreviaDeImportacion vistaPrevia(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return importacion.vistaPrevia(archivo.getInputStream());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Crea las filas validas y omite las que tienen error")
    public ResultadoDeImportacion importar(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return importacion.importar(archivo.getInputStream());
    }

    @PostMapping("/omitidas")
    @Operation(summary = "Las filas omitidas con su error, en .xlsx para corregir y volver a subir")
    public ResponseEntity<byte[]> omitidas(@RequestBody List<FilaImportada> omitidas) {
        return archivo(importacion.omitidasComoExcel(omitidas), "filas-con-error.xlsx");
    }

    private static ResponseEntity<byte[]> archivo(byte[] contenido, String nombre) {
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }
}
