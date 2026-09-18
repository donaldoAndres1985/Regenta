package com.regenta.reportes.aplicacion;

import java.util.Locale;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * HU-099 criterio 3: cada negocio ve las métricas de su patrón y solo esas.
 *
 * <p>Responde 404 y no 403 a propósito. No es que al dueño de la ferretería le
 * falte un permiso para ver la ocupación: es que en una ferretería la
 * ocupación no existe. Un 403 le diría "esto existe, pídeselo a tu jefe", y
 * eso sería falso.
 */
final class PatronDelNegocio {

    private PatronDelNegocio() {
    }

    static void exigir(String patron, String queMetrica) {
        String elSuyo = ContextoDeNegocio.actual().patron();
        if (elSuyo == null || !elSuyo.trim().toUpperCase(Locale.ROOT).equals(patron)) {
            throw new NoEncontradoException(
                    "Este negocio no lleva " + queMetrica + ": es del patrón " + elSuyo);
        }
    }
}
