package com.regenta.reportes.aplicacion;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Convierte filas en un archivo (HU-100 criterio 1). No sabe de dónde salen
 * las filas ni quién las pidió: recibe columnas y valores y devuelve bytes.
 */
@Component
public class ExportadorDeReportes {

    private static final char SEPARADOR = ';';

    public byte[] generar(String formato, String titulo, List<String> columnas,
            List<List<Object>> filas) {
        return switch (formato == null ? "" : formato.trim().toUpperCase(Locale.ROOT)) {
            case "CSV" -> csv(columnas, filas);
            case "XLSX" -> xlsx(titulo, columnas, filas);
            case "PDF" -> pdf(titulo, columnas, filas);
            default -> throw new ReglaDeNegocioException(
                    "Formato no soportado: " + formato + ". Son PDF, XLSX o CSV");
        };
    }

    public String tipoMime(String formato) {
        return switch (formato.trim().toUpperCase(Locale.ROOT)) {
            case "CSV" -> "text/csv; charset=utf-8";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PDF" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }

    /** Sin acentos, sin espacios y sin nada que parezca una ruta. */
    public String nombreDeArchivo(String titulo, String formato) {
        String base = Normalizer.normalize(titulo == null ? "reporte" : titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (base.isBlank()) {
            base = "reporte";
        }
        return base + "." + formato.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Separador punto y coma y BOM al principio: es lo que abre bien un Excel
     * en español, que es donde va a terminar este archivo. Sin BOM, los
     * acentos salen rotos y el que lo recibe cree que el reporte está mal.
     */
    private byte[] csv(List<String> columnas, List<List<Object>> filas) {
        StringBuilder texto = new StringBuilder("﻿");
        texto.append(String.join(String.valueOf(SEPARADOR), columnas.stream()
                .map(ExportadorDeReportes::celdaCsv).toList())).append("\r\n");
        for (List<Object> fila : filas) {
            texto.append(String.join(String.valueOf(SEPARADOR),
                    fila.stream().map(ExportadorDeReportes::celdaCsv).toList())).append("\r\n");
        }
        return texto.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String celdaCsv(Object valor) {
        String texto = valor == null ? "" : valor.toString();
        if (texto.indexOf(SEPARADOR) < 0 && texto.indexOf('"') < 0 && texto.indexOf('\n') < 0) {
            return texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private byte[] xlsx(String titulo, List<String> columnas, List<List<Object>> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(hojaValida(titulo));
            Row cabecera = hoja.createRow(0);
            for (int c = 0; c < columnas.size(); c++) {
                cabecera.createCell(c).setCellValue(columnas.get(c));
            }
            int numeroDeFila = 1;
            for (List<Object> fila : filas) {
                Row renglon = hoja.createRow(numeroDeFila++);
                for (int c = 0; c < fila.size(); c++) {
                    escribir(renglon.createCell(c), fila.get(c));
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (Exception fallo) {
            throw new ReglaDeNegocioException("No se pudo generar el XLSX: " + fallo.getMessage());
        }
    }

    /** Un número tiene que llegar como número: si va como texto, nadie lo puede sumar. */
    private static void escribir(Cell celda, Object valor) {
        if (valor == null) {
            celda.setBlank();
        } else if (valor instanceof Number numero) {
            celda.setCellValue(numero.doubleValue());
        } else if (valor instanceof Boolean si) {
            celda.setCellValue(si);
        } else {
            celda.setCellValue(valor.toString());
        }
    }

    private static String hojaValida(String titulo) {
        String limpio = (titulo == null ? "Reporte" : titulo).replaceAll("[\\\\/?*\\[\\]:]", " ");
        return limpio.length() > 31 ? limpio.substring(0, 31) : limpio;
    }

    /**
     * Una tabla simple, en horizontal y con fuente monoespaciada. No pretende
     * ser una maqueta: es un reporte para imprimir o adjuntar, y lo que
     * importa es que las columnas queden alineadas y que quepan las filas.
     */
    private byte[] pdf(String titulo, List<String> columnas, List<List<Object>> filas) {
        try (PDDocument documento = new PDDocument();
                ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var normal = new PDType1Font(Standard14Fonts.FontName.COURIER);
            var negrita = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            final float margen = 40;
            final float alto = 14;
            int porPagina = 48;

            for (int desde = 0; desde == 0 || desde < filas.size(); desde += porPagina) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                documento.addPage(pagina);
                try (PDPageContentStream lienzo = new PDPageContentStream(documento, pagina)) {
                    float y = pagina.getMediaBox().getHeight() - margen;
                    lienzo.beginText();
                    lienzo.setFont(negrita, 13);
                    lienzo.newLineAtOffset(margen, y);
                    lienzo.showText(sinRaros(titulo));
                    lienzo.endText();

                    y -= alto * 2;
                    escribirLinea(lienzo, negrita, margen, y, columnas.stream()
                            .map(ExportadorDeReportes::sinRaros).toList());
                    y -= alto;
                    int hasta = Math.min(filas.size(), desde + porPagina);
                    for (int f = desde; f < hasta; f++) {
                        escribirLinea(lienzo, normal, margen, y, filas.get(f).stream()
                                .map(v -> sinRaros(v == null ? "" : v.toString())).toList());
                        y -= alto;
                    }
                }
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (Exception fallo) {
            throw new ReglaDeNegocioException("No se pudo generar el PDF: " + fallo.getMessage());
        }
    }

    private static void escribirLinea(PDPageContentStream lienzo,
            org.apache.pdfbox.pdmodel.font.PDFont fuente, float x, float y, List<String> celdas)
            throws java.io.IOException {
        lienzo.beginText();
        lienzo.setFont(fuente, 9);
        lienzo.newLineAtOffset(x, y);
        StringBuilder linea = new StringBuilder();
        for (String celda : celdas) {
            String recortada = celda.length() > 28 ? celda.substring(0, 27) + "…" : celda;
            linea.append(String.format("%-30s", recortada));
        }
        lienzo.showText(sinRaros(linea.toString()));
        lienzo.endText();
    }

    /**
     * Las fuentes Standard 14 de PDF solo llevan WinAnsi: un carácter fuera de
     * ahí revienta la generación entera. Antes de perder el reporte por una
     * comilla tipográfica, se cambia por su equivalente.
     */
    private static String sinRaros(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder limpio = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            limpio.append(c >= 32 && c <= 255 ? c : '?');
        }
        return limpio.toString();
    }
}
