package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.regenta.inventario.domain.TipoAtributo;

/**
 * Un atributo tal como queda. {@code advertencia} solo trae texto cuando el
 * atributo se marco obligatorio y ya hay productos sin el (HU-027, criterio 3):
 * esos productos conservan lo que tienen.
 */
public record AtributoDeCategoria(
        UUID id,
        UUID categoriaId,
        String nombreCampo,
        String etiqueta,
        TipoAtributo tipo,
        boolean obligatorio,
        String unidad,
        List<String> opciones,
        String valorDefault,
        String validacionRegex,
        BigDecimal valorMin,
        BigDecimal valorMax,
        boolean heredable,
        int orden,
        String advertencia) {
}
