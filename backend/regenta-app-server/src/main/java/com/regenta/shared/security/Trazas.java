package com.regenta.shared.security;

import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

public final class Trazas {
    public static final String CABECERA = "X-Regenta-Trace-Id";

    private Trazas() {}

    public static String de(HttpServletRequest peticion) {
        String traza = peticion.getHeader(CABECERA);
        if (traza != null && !traza.isBlank()) {
            return traza.trim();
        }
        return UUID.randomUUID().toString();
    }
}
