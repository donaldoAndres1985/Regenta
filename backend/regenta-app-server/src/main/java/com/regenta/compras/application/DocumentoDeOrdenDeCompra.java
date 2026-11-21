package com.regenta.compras.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.regenta.usuarios.api.ConfiguracionNegocioApi.DatosNegocioTicket;

/**
 * HU-174 C4 · El PDF de una orden de compra para mandarle al proveedor: quién compra, a quién, qué
 * (con el código con que lo vende el proveedor), cuánto y para cuándo. Una página A4; si las líneas no
 * caben, sigue en las siguientes.
 */
final class DocumentoDeOrdenDeCompra {

    private static final float MARGEN = 40;
    private static final float ALTO_LINEA = 14;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Dónde empieza cada columna (Código, Producto, Cant., Costo, Desc., IVA, Total) y el borde derecho. */
    private static final float[] COLUMNAS = { MARGEN, 100, 300, 345, 420, 460, 495, 560 };

    private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private PDDocument documento;
    private PDPageContentStream lienzo;
    private float y;

    byte[] generar(DatosNegocioTicket negocio, OrdenDelNegocio orden, String proveedor, String nitProveedor,
            String contactoProveedor) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            this.documento = doc;
            nuevaPagina();

            String comprador = negocio == null ? "" : primeroNoVacio(negocio.nombreComercial(), negocio.razonSocial());
            renglon(negrita, 15, comprador);
            if (negocio != null) {
                renglon(normal, 9, unir(" · ", siHay("NIT ", negocio.nit()), negocio.direccion(), negocio.ciudad()));
                renglon(normal, 9, unir(" · ", negocio.telefono(), negocio.email()));
            }
            y -= 10;
            renglon(negrita, 13, "ORDEN DE COMPRA " + orden.numero());
            renglon(normal, 9, unir(" · ", "Emitida el " + FECHA.format(orden.fechaEmision()),
                    orden.fechaEsperada() == null ? null : "Entregar el " + FECHA.format(orden.fechaEsperada())));
            y -= 6;
            renglon(negrita, 10, "Proveedor: " + nulo(proveedor));
            renglon(normal, 9, unir(" · ", siHay("NIT ", nitProveedor), siHay("Contacto: ", contactoProveedor)));
            y -= 10;

            encabezadoDeTabla();
            for (OrdenDelNegocio.LineaDeOrden l : orden.lineas()) {
                if (y < MARGEN + 120) {
                    nuevaPagina();
                    encabezadoDeTabla();
                }
                fila(normal, List.of(nulo(l.codigoProveedor()), recortar(l.nombre(), 42), cantidad(l.cantidadPedida()),
                        pesos(l.costoUnitario()), porcentaje(l.descuentoPct()), porcentaje(l.impuestoPct()),
                        pesos(l.total())));
            }
            y -= 6;
            linea();
            y -= 4;
            total("Subtotal", orden.subtotal());
            if (orden.descuentoTotal().signum() > 0) {
                total("Descuento", orden.descuentoTotal().negate());
            }
            total("IVA", orden.impuestoTotal());
            if (orden.flete().signum() > 0) {
                total("Flete", orden.flete());
            }
            total("TOTAL", orden.total());
            if (orden.observaciones() != null && !orden.observaciones().isBlank()) {
                y -= 10;
                renglon(negrita, 9, "Observaciones");
                for (String parte : partir(orden.observaciones(), 100)) {
                    renglon(normal, 9, parte);
                }
            }
            lienzo.close();
            doc.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la orden " + orden.numero(), e);
        }
    }

    private void nuevaPagina() throws IOException {
        if (lienzo != null) {
            lienzo.close();
        }
        PDPage pagina = new PDPage(PDRectangle.A4);
        documento.addPage(pagina);
        lienzo = new PDPageContentStream(documento, pagina);
        y = pagina.getMediaBox().getHeight() - MARGEN;
    }

    private void encabezadoDeTabla() throws IOException {
        fila(negrita, List.of("Código", "Producto", "Cant.", "Costo", "Desc.", "IVA", "Total"));
        linea();
        y -= 4;
    }

    private void fila(PDFont fuente, List<String> celdas) throws IOException {
        for (int i = 0; i < celdas.size(); i++) {
            boolean numero = i >= 2;
            float x = numero ? COLUMNAS[i + 1] - ancho(fuente, 9, celdas.get(i)) - 4 : COLUMNAS[i];
            texto(fuente, 9, x, celdas.get(i));
        }
        y -= ALTO_LINEA;
    }

    private void total(String etiqueta, BigDecimal valor) throws IOException {
        PDFont fuente = "TOTAL".equals(etiqueta) ? negrita : normal;
        texto(fuente, 10, COLUMNAS[4], etiqueta);
        String monto = pesos(valor);
        texto(fuente, 10, COLUMNAS[7] - ancho(fuente, 10, monto) - 4, monto);
        y -= ALTO_LINEA;
    }

    private void renglon(PDFont fuente, float tamano, String texto) throws IOException {
        if (texto == null || texto.isBlank()) {
            return;
        }
        texto(fuente, tamano, MARGEN, texto);
        y -= tamano + 4;
    }

    private void texto(PDFont fuente, float tamano, float x, String texto) throws IOException {
        lienzo.beginText();
        lienzo.setFont(fuente, tamano);
        lienzo.newLineAtOffset(x, y);
        lienzo.showText(soloWinAnsi(texto));
        lienzo.endText();
    }

    private void linea() throws IOException {
        lienzo.moveTo(MARGEN, y + 10);
        lienzo.lineTo(COLUMNAS[7], y + 10);
        lienzo.stroke();
    }

    private float ancho(PDFont fuente, float tamano, String texto) throws IOException {
        return fuente.getStringWidth(soloWinAnsi(texto)) / 1000 * tamano;
    }

    /** Las fuentes estándar solo traen WinAnsi (tildes y ñ sí): lo demás se reemplaza. */
    static String soloWinAnsi(String texto) {
        if (texto == null) {
            return "";
        }
        StringBuilder limpio = new StringBuilder();
        for (char c : texto.toCharArray()) {
            limpio.append(c == '\n' || c == '\r' || c == '\t' ? ' ' : (c < 256 ? c : '?'));
        }
        return limpio.toString();
    }

    static String pesos(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO")));
        BigDecimal redondeado = valor == null ? BigDecimal.ZERO : valor.setScale(0, RoundingMode.HALF_UP);
        String texto = formato.format(redondeado.abs()).replace(',', '.');
        return (redondeado.signum() < 0 ? "-$" : "$") + texto;
    }

    private static String cantidad(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }

    private static String porcentaje(BigDecimal v) {
        return v == null || v.signum() == 0 ? "" : v.stripTrailingZeros().toPlainString() + "%";
    }

    private static String recortar(String v, int largo) {
        String t = nulo(v);
        return t.length() <= largo ? t : t.substring(0, largo - 3) + "...";
    }

    private static List<String> partir(String texto, int largo) {
        List<String> partes = new ArrayList<>();
        String resto = texto.trim();
        while (resto.length() > largo) {
            int corte = resto.lastIndexOf(' ', largo);
            corte = corte <= 0 ? largo : corte;
            partes.add(resto.substring(0, corte));
            resto = resto.substring(corte).trim();
        }
        partes.add(resto);
        return partes;
    }

    private static String nulo(String v) {
        return v == null ? "" : v;
    }

    private static String siHay(String prefijo, String v) {
        return v == null || v.isBlank() ? null : prefijo + v;
    }

    private static String primeroNoVacio(String a, String b) {
        return a != null && !a.isBlank() ? a : nulo(b);
    }

    private static String unir(String separador, String... partes) {
        List<String> hay = new ArrayList<>();
        for (String p : partes) {
            if (p != null && !p.isBlank()) {
                hay.add(p);
            }
        }
        return String.join(separador, hay);
    }
}
