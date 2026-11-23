package com.regenta.facturacion.application;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HU-117 C4 · Un rechazo de la DIAN dice qué dato lo causó, no solo que falló. La DIAN responde líneas
 * como {@code "Regla: FAK21, Rechazo: ID de adquiriente no Informado"}. Las reglas {@code FAJ} son del
 * emisor ({@code AccountingSupplierParty}) y las {@code FAK} del adquiriente
 * ({@code AccountingCustomerParty}), y en las dos el número dice el elemento (anexo técnico 1.9): 20 el
 * nombre, 21 el documento, 24 y 25 el dígito de verificación, 26 las responsabilidades, del 28 en adelante
 * la dirección fiscal, 43 y 44 el nombre y el documento de la entidad legal. Aquí se traduce a lo que el
 * negocio puede corregir y dónde.
 *
 * <p>Las notificaciones (las líneas con {@code Notificación}) no rechazan: se guardan, pero no van al
 * mensaje.
 */
public final class RechazosDeLaDian {

    private static final Pattern REGLA = Pattern.compile("Regla:\\s*([A-Z]{2,4}\\d+[a-z]?)\\s*,\\s*([^:]+):\\s*(.*)",
            Pattern.DOTALL);
    private static final Pattern PARTE = Pattern.compile("^(FAJ|FAK)(\\d+)([a-z]?)$");

    private RechazosDeLaDian() {
    }

    /** El código y el mensaje que quedan en la factura rechazada. */
    public record Rechazo(String codigo, String mensaje) {
    }

    public static Rechazo explicar(String codigoEstado, String descripcion, List<String> errores) {
        List<String> codigos = new ArrayList<>();
        List<String> mensajes = new ArrayList<>();
        for (String linea : errores == null ? List.<String>of() : errores) {
            Matcher m = REGLA.matcher(linea == null ? "" : linea.trim());
            if (!m.matches()) {
                continue;
            }
            if (m.group(2).trim().toLowerCase().startsWith("notifica")) {
                continue;
            }
            String regla = m.group(1);
            codigos.add(regla);
            mensajes.add(campo(regla) + ": " + m.group(3).trim() + " (regla " + regla + ")");
        }
        if (codigos.isEmpty()) {
            String texto = descripcion == null || descripcion.isBlank() ? "Documento rechazado por la DIAN" : descripcion;
            List<String> otros = errores == null ? List.of() : errores.stream()
                    .filter(e -> e != null && !e.isBlank() && !e.toLowerCase().contains("notificaci")).toList();
            if (!otros.isEmpty()) {
                texto = texto + ": " + String.join("; ", otros);
            }
            return new Rechazo(codigoEstado == null || codigoEstado.isBlank() ? "99" : codigoEstado, texto);
        }
        return new Rechazo(codigos.get(0), String.join("; ", mensajes));
    }

    /** Qué dato y dónde se corrige, según la regla. */
    static String campo(String regla) {
        Matcher m = PARTE.matcher(regla);
        if (!m.matches()) {
            return "La factura";
        }
        boolean emisor = "FAJ".equals(m.group(1));
        String quien = emisor ? "Emisor" : "Adquiriente";
        String donde = emisor ? " (datos fiscales del negocio)" : " (datos del cliente)";
        int n = Integer.parseInt(m.group(2));
        String que;
        if (n == 20 || n == 43) {
            que = emisor ? "razón social" : "nombre o razón social";
        } else if (n == 21 || n == 22 || n == 23 || n == 44 || n == 45 || n == 46) {
            que = emisor ? "NIT" : "número de documento";
        } else if (n == 24 || n == 25 || n == 47 || n == 48) {
            que = "dígito de verificación";
        } else if (n == 26 || n == 27) {
            que = "responsabilidades fiscales";
        } else if (n >= 28 && n <= 42) {
            que = "dirección fiscal (municipio, departamento, dirección)";
        } else if (n >= 7 && n <= 19) {
            que = "dirección";
        } else if (n >= 49) {
            que = "contacto (correo o teléfono)";
        } else {
            que = "identificación";
        }
        return quien + " · " + que + donde;
    }
}
