package com.regenta.alertas.domain;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Sustituye los marcadores {@code {clave}} de una plantilla con los campos del hecho. */
public final class PlantillaDeAlerta {

    private static final Pattern MARCADOR = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private PlantillaDeAlerta() {
    }

    public static String render(String plantilla, Map<String, Object> campos) {
        if (plantilla == null) {
            return "";
        }
        Matcher m = MARCADOR.matcher(plantilla);
        StringBuilder salida = new StringBuilder();
        while (m.find()) {
            Object valor = campos.get(m.group(1));
            m.appendReplacement(salida,
                    Matcher.quoteReplacement(valor == null ? "{" + m.group(1) + "}" : valor.toString()));
        }
        m.appendTail(salida);
        return salida.toString();
    }
}
