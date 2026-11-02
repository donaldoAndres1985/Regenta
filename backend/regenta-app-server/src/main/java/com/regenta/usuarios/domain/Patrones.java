package com.regenta.usuarios.domain;

import java.util.Set;

/**
 * Los tres patrones operativos. No hay un cuarto: el tipo de negocio se
 * resuelve con filas, el patron con tablas.
 */
public final class Patrones {

    public static final String VENTA_DIRECTA = "VENTA_DIRECTA";
    public static final String RESERVA = "RESERVA";
    public static final String COMANDA = "COMANDA";

    public static final Set<String> TODOS = Set.of(VENTA_DIRECTA, RESERVA, COMANDA);

    private Patrones() {
    }

    public static boolean existe(String patron) {
        return patron != null && TODOS.contains(patron);
    }
}
