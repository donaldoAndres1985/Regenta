package com.regenta.comun.dominio;

import java.util.UUID;

/**
 * Tenant de la petición en curso. Lo puebla el filtro que lee el JWT.
 * Se limpia SIEMPRE en un finally: el pool de hilos se reutiliza entre
 * peticiones de negocios distintos y un ThreadLocal sucio es una fuga de
 * datos entre tenants.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID>   NEGOCIO = new ThreadLocal<>();
    private static final ThreadLocal<UUID>   USUARIO = new ThreadLocal<>();
    private static final ThreadLocal<String> PLAN    = new ThreadLocal<>();
    private static final ThreadLocal<String> PATRON  = new ThreadLocal<>();

    private TenantContext() {}

    public static void establecer(UUID negocio, UUID usuario, String plan, String patron) {
        NEGOCIO.set(negocio); USUARIO.set(usuario); PLAN.set(plan); PATRON.set(patron);
    }

    public static UUID negocioActual() {
        UUID n = NEGOCIO.get();
        if (n == null) throw new IllegalStateException(
            "Sin tenant en contexto: toda operación debe ejecutarse dentro de un negocio");
        return n;
    }

    public static UUID   usuarioActual() { return USUARIO.get(); }
    public static String planActual()    { return PLAN.get(); }
    public static String patronActual()  { return PATRON.get(); }

    public static void limpiar() {
        NEGOCIO.remove(); USUARIO.remove(); PLAN.remove(); PATRON.remove();
    }
}
