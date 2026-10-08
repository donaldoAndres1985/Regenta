package com.regenta.comun.auditoria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * El detalle que un servicio adjunta a su evento para que la bitácora diga qué
 * cambió y no solo que algo cambió (HU-130).
 *
 * <p>Viaja dentro del payload bajo la clave {@link #CLAVE}:
 * <pre>{"_auditoria": {"tipo": "ACTUALIZAR",
 *                    "cambios": {"precio": {"antes": 100, "despues": 120}}}}</pre>
 * servicio-auditoria lo lee de ahí. Un servicio que no lo adjunta sigue
 * auditado, sin detalle: el evento no se pierde por no estar instrumentado.
 *
 * <p>Los campos sensibles se enmascaran aquí, antes de que el valor salga del
 * servicio, y servicio-auditoria vuelve a enmascarar por si algún emisor se
 * olvidó: el valor de una clave no debe quedar en claro en ningún lado.
 */
public final class CambiosDeAuditoria {

    /** La clave del payload donde viaja el detalle. */
    public static final String CLAVE = "_auditoria";

    /** Lo que se escribe en lugar del valor de un campo sensible. */
    public static final String OCULTO = "***";

    /**
     * Palabras que hacen sensible a un campo. Se comparan contra cada palabra
     * del nombre ({@code pin_caja}, {@code passwordHash}), no como subcadena:
     * {@code "pin"} no debe ocultar {@code shipping}.
     */
    private static final Set<String> SENSIBLES = Set.of("password", "contrasena", "clave", "secret",
            "secreto", "token", "pin", "hash", "certificado", "tarjeta", "cvv");

    private CambiosDeAuditoria() {
    }

    /** Criterio 3: un alta. Todos los campos, con {@code antes} vacío. */
    public static Map<String, Object> creacion(Map<String, ?> despues) {
        return bloque("CREAR", diff(Map.of(), despues));
    }

    /** Criterio 1: una edición. Solo los campos que cambiaron. */
    public static Map<String, Object> actualizacion(Map<String, ?> antes, Map<String, ?> despues) {
        return bloque("ACTUALIZAR", diff(antes, despues));
    }

    /** Criterio 3: una baja. Todos los campos, con {@code despues} vacío. */
    public static Map<String, Object> eliminacion(Map<String, ?> antes) {
        return bloque("ELIMINAR", diff(antes, Map.of()));
    }

    /** Si el nombre de un campo lo hace sensible (criterio 2). */
    public static boolean esSensible(String campo) {
        if (campo == null) {
            return false;
        }
        String separado = campo.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        for (String palabra : separado.split("[^a-z0-9]+")) {
            if (SENSIBLES.contains(palabra)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copia de {@code valor} con el valor de cada campo sensible reemplazado
     * por {@link #OCULTO}, a cualquier profundidad.
     */
    public static Object enmascarar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> copia = new LinkedHashMap<>();
            mapa.forEach((k, v) -> {
                String campo = String.valueOf(k);
                copia.put(campo, esSensible(campo) && v != null && !(v instanceof Map<?, ?>)
                        ? OCULTO : enmascarar(v));
            });
            return copia;
        }
        if (valor instanceof Collection<?> lista) {
            List<Object> copia = new ArrayList<>();
            lista.forEach(v -> copia.add(enmascarar(v)));
            return copia;
        }
        return valor;
    }

    private static Map<String, Object> bloque(String tipo, Map<String, Object> cambios) {
        Map<String, Object> bloque = new LinkedHashMap<>();
        bloque.put("tipo", tipo);
        bloque.put("cambios", cambios);
        return bloque;
    }

    private static Map<String, Object> diff(Map<String, ?> antes, Map<String, ?> despues) {
        Set<String> campos = new LinkedHashSet<>();
        campos.addAll(antes.keySet());
        campos.addAll(despues.keySet());
        campos.remove(CLAVE);
        Map<String, Object> cambios = new LinkedHashMap<>();
        for (String campo : campos) {
            Object a = antes.get(campo);
            Object d = despues.get(campo);
            if (iguales(a, d)) {
                continue;
            }
            Map<String, Object> par = new LinkedHashMap<>();
            boolean oculto = esSensible(campo);
            par.put("antes", oculto && a != null ? OCULTO : enmascarar(a));
            par.put("despues", oculto && d != null ? OCULTO : enmascarar(d));
            cambios.put(campo, par);
        }
        return cambios;
    }

    /** {@code 100} y {@code 100.00} son el mismo precio, aunque uno venga como Integer. */
    private static boolean iguales(Object a, Object b) {
        if (a instanceof Number x && b instanceof Number y) {
            return new BigDecimal(x.toString()).compareTo(new BigDecimal(y.toString())) == 0;
        }
        return Objects.equals(a, b);
    }
}
