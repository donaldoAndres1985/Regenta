package com.regenta.alertas.aplicacion;

import com.regenta.alertas.domain.TipoAlerta;

/** Un tipo del catálogo, para poblar el selector al crear una regla (HU-092 criterio 1). */
public record TipoDeAlerta(
        String codigo,
        String nombre,
        String modulo,
        String patron,
        String severidadDefault) {

    static TipoDeAlerta de(TipoAlerta t) {
        return new TipoDeAlerta(t.getCodigo(), t.getNombre(), t.getModulo(), t.getPatron(),
                t.getSeveridadDefault().name());
    }
}
