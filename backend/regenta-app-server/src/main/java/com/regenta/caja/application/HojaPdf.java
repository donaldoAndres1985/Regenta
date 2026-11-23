package com.regenta.caja.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * HU-144 · Un PDF sencillo de varias páginas para los informes de caja: renglones, pares
 * etiqueta-valor y tablas con las cifras a la derecha. Cuando no cabe, sigue en otra página y repite
 * el encabezado de la tabla. Las fuentes Standard 14 solo llevan WinAnsi: lo demás se reemplaza.
 */
final class HojaPdf implements AutoCloseable {

    static final float MARGEN = 40;
    private static final float ALTO = 14;

    /** Una columna de tabla: dónde empieza, dónde termina y si va alineada a la derecha. */
    record Columna(String titulo, float desde, float hasta, boolean derecha) {
    }

    private final PDDocument documento = new PDDocument();
    private final PDRectangle tamano;
    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private PDPageContentStream lienzo;
    private float y;
    private int paginas;
    private List<Columna> tablaActual;

    HojaPdf(boolean horizontal) {
        this.tamano = horizontal ? new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth())
                : PDRectangle.A4;
    }

    float ancho() {
        return tamano.getWidth() - MARGEN;
    }

    void titulo(String texto) throws IOException {
        renglon(texto, 14, true);
    }

    void subtitulo(String texto) throws IOException {
        renglon(texto, 9, false);
    }

    void seccion(String texto) throws IOException {
        espacio(8);
        renglon(texto, 11, true);
        linea();
    }

    void espacio(float puntos) throws IOException {
        asegurar(puntos);
        y -= puntos;
    }

    void renglon(String texto, float tamanoLetra, boolean conNegrita) throws IOException {
        asegurar(ALTO + 2);
        escribir(conNegrita ? negrita : normal, tamanoLetra, MARGEN, texto);
        y -= tamanoLetra + 5;
    }

    /** Etiqueta a la izquierda y valor a la derecha, en la mitad izquierda de la hoja. */
    void par(String etiqueta, String valor, boolean conNegrita) throws IOException {
        asegurar(ALTO);
        PDFont fuente = conNegrita ? negrita : normal;
        escribir(fuente, 10, MARGEN, etiqueta);
        float derecha = MARGEN + 300;
        escribir(fuente, 10, derecha - ancho(fuente, 10, valor), valor);
        y -= ALTO;
    }

    void tabla(List<Columna> columnas) throws IOException {
        this.tablaActual = columnas;
        asegurar(ALTO * 2);
        encabezadoDeTabla();
    }

    void fila(List<String> celdas, boolean conNegrita) throws IOException {
        if (y - ALTO < MARGEN) {
            nuevaPagina();
            encabezadoDeTabla();
        }
        PDFont fuente = conNegrita ? negrita : normal;
        for (int i = 0; i < tablaActual.size() && i < celdas.size(); i++) {
            Columna c = tablaActual.get(i);
            String texto = recortar(fuente, 9, celdas.get(i) == null ? "" : celdas.get(i), c.hasta() - c.desde() - 4);
            float x = c.derecha() ? c.hasta() - ancho(fuente, 9, texto) : c.desde();
            escribir(fuente, 9, x, texto);
        }
        y -= ALTO;
    }

    void linea() throws IOException {
        // Entre el renglón anterior (su base está ALTO más arriba) y el siguiente, sin tocar ninguno.
        asegurar(4);
        lienzo.moveTo(MARGEN, y + 10);
        lienzo.lineTo(ancho(), y + 10);
        lienzo.stroke();
        y -= 4;
    }

    byte[] bytes() throws IOException {
        if (lienzo == null) {
            nuevaPagina();
        }
        lienzo.close();
        lienzo = null;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        documento.save(salida);
        return salida.toByteArray();
    }

    int paginas() {
        return paginas;
    }

    @Override
    public void close() throws IOException {
        if (lienzo != null) {
            lienzo.close();
        }
        documento.close();
    }

    // ---------------------------------------------------------------------------------------------

    private void encabezadoDeTabla() throws IOException {
        for (Columna c : tablaActual) {
            float x = c.derecha() ? c.hasta() - ancho(negrita, 9, c.titulo()) : c.desde();
            escribir(negrita, 9, x, c.titulo());
        }
        y -= ALTO;
        linea();
    }

    private void asegurar(float alto) throws IOException {
        if (lienzo == null || y - alto < MARGEN) {
            nuevaPagina();
        }
    }

    private void nuevaPagina() throws IOException {
        if (lienzo != null) {
            lienzo.close();
        }
        PDPage pagina = new PDPage(tamano);
        documento.addPage(pagina);
        lienzo = new PDPageContentStream(documento, pagina);
        lienzo.setLineWidth(0.5f);
        paginas++;
        y = tamano.getHeight() - MARGEN;
    }

    private void escribir(PDFont fuente, float tamanoLetra, float x, String texto) throws IOException {
        lienzo.beginText();
        lienzo.setFont(fuente, tamanoLetra);
        lienzo.newLineAtOffset(x, y);
        lienzo.showText(sinRaros(texto));
        lienzo.endText();
    }

    private static float ancho(PDFont fuente, float tamanoLetra, String texto) throws IOException {
        return fuente.getStringWidth(sinRaros(texto)) / 1000 * tamanoLetra;
    }

    private static String recortar(PDFont fuente, float tamanoLetra, String texto, float maximo) throws IOException {
        String t = texto;
        while (t.length() > 1 && ancho(fuente, tamanoLetra, t) > maximo) {
            t = t.substring(0, t.length() - 2) + "…";
        }
        return t;
    }

    /** Lo que no esté en WinAnsi revienta el PDF entero: se cambia por su equivalente o por «?». */
    static String sinRaros(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder limpio = new StringBuilder(texto.length());
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '−', '–', '—' -> limpio.append('-');
                case '«', '»' -> limpio.append('"');
                case '…' -> limpio.append("...");
                case '·' -> limpio.append('·');
                default -> limpio.append(c >= 32 && c <= 255 ? c : '?');
            }
        }
        return limpio.toString();
    }
}
