package com.regenta.auditoria.aplicacion;

import java.util.Locale;

/**
 * Traduce el nombre de un evento de dominio (p. ej. {@code venta_completada})
 * a una de las acciones que acepta el CHECK de {@code eventos_auditoria.accion}
 * (HU-101). Es una heurística sobre el nombre del evento, no una regla de
 * cada servicio: capturar "cualquier cambio en cualquier servicio" sin tocar
 * los otros catorce solo es posible escuchando lo que ya publican.
 */
public final class AccionDeAuditoria {

    private AccionDeAuditoria() {
    }

    public static String desde(String tipoEvento) {
        if (tipoEvento == null || tipoEvento.isBlank()) {
            return "ACTUALIZAR";
        }
        String t = tipoEvento.toLowerCase(Locale.ROOT);
        if (t.equals("login_fallido")) {
            return "LOGIN_FALLIDO";
        }
        if (t.contains("login")) {
            return "LOGIN";
        }
        if (t.contains("logout")) {
            return "LOGOUT";
        }
        if (t.contains("aprobad") || t.contains("autorizad")) {
            return "APROBAR";
        }
        if (t.contains("anulad") || t.contains("cancelad") || t.contains("rechazad")) {
            return "ANULAR";
        }
        if (t.contains("eliminad") || t.contains("borrad")) {
            return "ELIMINAR";
        }
        if (t.contains("creado") || t.contains("creada") || t.contains("registrado")
                || t.contains("registrada")) {
            return "CREAR";
        }
        return "ACTUALIZAR";
    }
}
