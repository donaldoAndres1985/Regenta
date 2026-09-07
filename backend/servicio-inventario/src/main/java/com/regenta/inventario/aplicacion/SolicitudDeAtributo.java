package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;

import com.regenta.inventario.domain.TipoAtributo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Define un campo extra de una categoria. El {@code nombreCampo} es un slug: es
 * la clave con la que el valor queda en el JSONB {@code productos.atributos}.
 */
public record SolicitudDeAtributo(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[a-z][a-z0-9_]*$",
                message = "el nombre del campo va en minusculas, sin espacios (ej. fecha_vencimiento)")
        String nombreCampo,
        @NotBlank @Size(max = 80) String etiqueta,
        @NotNull TipoAtributo tipo,
        boolean obligatorio,
        @Size(max = 20) String unidad,
        List<@NotBlank String> opciones,
        String valorDefault,
        String validacionRegex,
        BigDecimal valorMin,
        BigDecimal valorMax,
        Boolean heredable,
        Integer orden) {
}
