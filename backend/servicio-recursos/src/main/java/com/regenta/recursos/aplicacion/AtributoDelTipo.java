package com.regenta.recursos.aplicacion;

import java.util.List;
import java.util.UUID;

import com.regenta.recursos.domain.AtributoTipoRecurso;

/** Un atributo de un tipo de recurso, como lo ve el administrador. */
public record AtributoDelTipo(
        UUID id,
        String nombreCampo,
        String etiqueta,
        String tipo,
        boolean obligatorio,
        List<String> opciones,
        int orden) {

    static AtributoDelTipo de(AtributoTipoRecurso a) {
        return new AtributoDelTipo(a.getId(), a.getNombreCampo(), a.getEtiqueta(),
                a.getTipo().name(), a.isObligatorio(), a.getOpciones(), a.getOrden());
    }
}
