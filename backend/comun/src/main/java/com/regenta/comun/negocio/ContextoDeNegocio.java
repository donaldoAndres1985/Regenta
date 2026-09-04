package com.regenta.comun.negocio;

import java.util.Optional;
import java.util.UUID;

/**
 * El negocio de la peticion en curso.
 *
 * <p>Se limpia SIEMPRE en un finally: el pool de hilos se reutiliza entre
 * peticiones de negocios distintos, y un ThreadLocal sucio no es un bug de
 * concurrencia cualquiera, es que un negocio vea los datos de otro.
 */
public final class ContextoDeNegocio {

    private static final ThreadLocal<DatosDelNegocio> ACTUAL = new ThreadLocal<>();

    private ContextoDeNegocio() {
    }

    public static void establecer(DatosDelNegocio datos) {
        ACTUAL.set(datos);
    }

    public static void limpiar() {
        ACTUAL.remove();
    }

    public static boolean hay() {
        return ACTUAL.get() != null;
    }

    public static Optional<DatosDelNegocio> quizas() {
        return Optional.ofNullable(ACTUAL.get());
    }

    /** El contexto de la peticion. Falla si no hay: nada se opera sin negocio. */
    public static DatosDelNegocio actual() {
        DatosDelNegocio datos = ACTUAL.get();
        if (datos == null) {
            throw new SinNegocioException(
                    "Sin negocio en contexto: toda operacion se ejecuta dentro de un negocio");
        }
        return datos;
    }

    public static UUID negocioActual() {
        return actual().negocio();
    }

    public static UUID usuarioActual() {
        return actual().usuario();
    }

    /** Ejecuta algo en nombre de un negocio y deja el contexto como estaba. */
    public static void en(DatosDelNegocio datos, Runnable tarea) {
        DatosDelNegocio anterior = ACTUAL.get();
        try {
            ACTUAL.set(datos);
            tarea.run();
        } finally {
            if (anterior == null) {
                ACTUAL.remove();
            } else {
                ACTUAL.set(anterior);
            }
        }
    }
}
