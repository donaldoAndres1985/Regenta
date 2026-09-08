package com.regenta.alertas.aplicacion;

import java.util.Map;
import java.util.UUID;

/**
 * Un hecho que puede disparar reglas de un tipo (HU-092): "el lote L de P vence
 * en 20 días", "el producto P quedó en 3". Lo arma el motor a partir de un
 * evento (HU-093) o de una llamada del sistema.
 */
public record HechoDeAlerta(
        String tipoCodigo,
        String entidadTipo,
        UUID entidadId,
        String rutaApp,
        UUID sucursalId,
        Map<String, Object> campos) {

    public HechoDeAlerta {
        campos = campos == null ? Map.of() : Map.copyOf(campos);
    }
}
