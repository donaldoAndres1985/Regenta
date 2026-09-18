package com.regenta.alertas.aplicacion;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Un hecho que puede disparar reglas de un tipo (HU-092): "el lote L de P vence
 * en 20 días", "el producto P quedó en 3". Lo arma el motor a partir de un
 * evento (HU-093) o de una llamada del sistema.
 *
 * @param usuariosExtra quien además de los destinatarios de la regla tiene que
 *        enterarse porque el hecho es suyo — el vendedor cuya venta offline
 *        chocó al subir, por ejemplo (HU-043 criterio 4). La regla dice a
 *        quién avisar siempre; esto, a quién avisar por este hecho concreto.
 */
public record HechoDeAlerta(
        String tipoCodigo,
        String entidadTipo,
        UUID entidadId,
        String rutaApp,
        UUID sucursalId,
        Map<String, Object> campos,
        Set<UUID> usuariosExtra) {

    public HechoDeAlerta {
        campos = campos == null ? Map.of() : Map.copyOf(campos);
        usuariosExtra = usuariosExtra == null ? Set.of() : Set.copyOf(usuariosExtra);
    }

    public HechoDeAlerta(String tipoCodigo, String entidadTipo, UUID entidadId, String rutaApp,
            UUID sucursalId, Map<String, Object> campos) {
        this(tipoCodigo, entidadTipo, entidadId, rutaApp, sucursalId, campos, Set.of());
    }
}
