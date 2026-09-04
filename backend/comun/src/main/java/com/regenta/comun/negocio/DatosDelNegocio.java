package com.regenta.comun.negocio;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Quien pide, en nombre de que negocio y con que puede. Sale del token que
 * valido el gateway, nunca de un parametro de la peticion.
 *
 * @param negocio    tenant dueño de todo lo que se toque en esta peticion
 * @param usuario    quien la hace
 * @param plan       BASICO | PROFESIONAL | EMPRESARIAL
 * @param patron     VENTA_DIRECTA | RESERVA | COMANDA
 * @param roles      roles del usuario en este negocio
 * @param modulos    modulos activos del negocio, ya resueltos por el emisor
 * @param permisos   permisos efectivos del usuario, union de los de sus roles
 * @param sucursales sucursales a las que esta acotado; vacio = todas
 */
public record DatosDelNegocio(
        UUID negocio,
        UUID usuario,
        String plan,
        String patron,
        Set<String> roles,
        Set<String> modulos,
        Set<String> permisos,
        Set<UUID> sucursales) {

    public DatosDelNegocio {
        roles = copiaEnMayuscula(roles);
        modulos = copiaEnMayuscula(modulos);
        permisos = copiaEnMayuscula(permisos);
        sucursales = sucursales == null ? Set.of() : Set.copyOf(sucursales);
    }

    private static Set<String> copiaEnMayuscula(Set<String> valores) {
        if (valores == null) {
            return Set.of();
        }
        Set<String> copia = new LinkedHashSet<>();
        valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.trim().toUpperCase(Locale.ROOT))
                .forEach(copia::add);
        return Set.copyOf(copia);
    }

    public boolean tieneModulo(String codigo) {
        return codigo != null && modulos.contains(codigo.trim().toUpperCase(Locale.ROOT));
    }

    /** HU-016: lo que puede hacer, no quien es. */
    public boolean puede(String permiso) {
        return permiso != null && permisos.contains(permiso.trim().toUpperCase(Locale.ROOT));
    }

    public boolean tieneRol(String codigo) {
        return codigo != null && roles.contains(codigo.trim().toUpperCase(Locale.ROOT));
    }

    /** Sin sucursales declaradas el usuario ve todas las de su negocio. */
    public boolean alcanzaTodaSucursal() {
        return sucursales.isEmpty();
    }

    public boolean alcanza(UUID sucursal) {
        return alcanzaTodaSucursal() || sucursales.contains(sucursal);
    }
}
