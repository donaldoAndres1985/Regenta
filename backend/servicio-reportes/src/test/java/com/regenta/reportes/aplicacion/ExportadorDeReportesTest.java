package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * HU-100 criterio 1: el mismo reporte, en los tres formatos. Sin Spring y sin
 * base: esto es convertir filas en bytes.
 */
class ExportadorDeReportesTest {

    private final ExportadorDeReportes exportador = new ExportadorDeReportes();

    private static final List<String> COLUMNAS = List.of("Categoría", "Monto", "Unidades");
    private static final List<List<Object>> FILAS = List.of(
            List.of("Herramientas", new BigDecimal("1250000.50"), 12),
            List.of("Tornillería; con punto y coma", new BigDecimal("98000"), 340));

    @Test
    @DisplayName("Criterio 1: CSV, con la cabecera y el separador escapado")
    void exportaCsv() {
        byte[] archivo = exportador.generar("CSV", "Ventas por categoría", COLUMNAS, FILAS);

        String texto = new String(archivo, StandardCharsets.UTF_8);
        assertThat(texto).startsWith("﻿");   // BOM: sin él Excel abre los acentos rotos
        assertThat(texto).contains("Categoría;Monto;Unidades");
        assertThat(texto).contains("Herramientas;1250000.50;12");
        assertThat(texto)
                .as("el valor trae el mismo separador: va entrecomillado o parte la fila")
                .contains("\"Tornillería; con punto y coma\"");
    }

    @Test
    @DisplayName("Criterio 1: XLSX que se puede volver a abrir, con los números como números")
    void exportaXlsx() throws Exception {
        byte[] archivo = exportador.generar("XLSX", "Ventas por categoría", COLUMNAS, FILAS);

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(archivo))) {
            var hoja = libro.getSheetAt(0);
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Categoría");
            assertThat(hoja.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Herramientas");
            assertThat(hoja.getRow(1).getCell(1).getNumericCellValue())
                    .as("un monto tiene que llegar como número, no como texto: si no, no se suma")
                    .isEqualTo(1250000.50);
            assertThat(hoja.getRow(2).getCell(2).getNumericCellValue()).isEqualTo(340);
        }
    }

    @Test
    @DisplayName("Criterio 1: PDF de verdad, con su cabecera de archivo")
    void exportaPdf() {
        byte[] archivo = exportador.generar("PDF", "Ventas por categoría", COLUMNAS, FILAS);

        assertThat(new String(archivo, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(archivo.length).isGreaterThan(400);
    }

    @Test
    @DisplayName("Un reporte sin filas se exporta igual: un archivo vacío no es un error")
    void exportaSinFilas() {
        for (String formato : List.of("CSV", "XLSX", "PDF")) {
            assertThat(exportador.generar(formato, "Vacío", COLUMNAS, List.of()))
                    .as(formato)
                    .isNotEmpty();
        }
    }

    @Test
    @DisplayName("Un formato que no existe se rechaza, no se inventa uno")
    void formatoDesconocido() {
        assertThatThrownBy(() -> exportador.generar("DOCX", "Ventas", COLUMNAS, FILAS))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El nombre del archivo lleva el formato y no deja pasar rutas")
    void nombreDeArchivo() {
        assertThat(exportador.nombreDeArchivo("Ventas por categoría", "XLSX"))
                .isEqualTo("ventas-por-categoria.xlsx");
        assertThat(exportador.nombreDeArchivo("../../etc/passwd", "CSV"))
                .doesNotContain("/")
                .doesNotContain("..");
    }
}
