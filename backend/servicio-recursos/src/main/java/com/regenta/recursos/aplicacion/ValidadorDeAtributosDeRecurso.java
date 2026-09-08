package com.regenta.recursos.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.recursos.domain.AtributoTipoRecurso;

/**
 * Valida el mapa {@code atributos} de un recurso contra lo que exige su tipo
 * (HU-064 criterio 4). PostgreSQL no comprueba la forma del JSON: esto es lo que
 * hace que "el consultorio exige registro sanitario" sea cierto sin programar
 * una columna. Espejo del {@code ValidadorDeAtributos} de Inventario.
 */
@Component
public class ValidadorDeAtributosDeRecurso {

    /** Devuelve el mapa normalizado o lanza 422 nombrando los campos que fallan. */
    public Map<String, Object> validar(List<AtributoTipoRecurso> definiciones,
            Map<String, Object> valores) {
        Map<String, Object> entrada = valores == null ? Map.of() : valores;
        Map<String, Object> salida = new LinkedHashMap<>(entrada);
        List<String> problemas = new ArrayList<>();

        for (AtributoTipoRecurso atributo : definiciones) {
            if (!atributo.isActivo()) {
                continue;
            }
            String campo = atributo.getNombreCampo();
            Object valor = entrada.get(campo);
            boolean vacio = valor == null || (valor instanceof String s && s.isBlank());

            if (vacio) {
                if (atributo.isObligatorio()) {
                    problemas.add("falta \"" + campo + "\"");
                }
                continue;
            }
            validarValor(atributo, valor, problemas);
        }

        if (!problemas.isEmpty()) {
            throw new ReglaDeNegocioException(
                    "Los atributos del tipo de recurso no cuadran: " + String.join("; ", problemas));
        }
        return salida;
    }

    private void validarValor(AtributoTipoRecurso atributo, Object valor, List<String> problemas) {
        String campo = atributo.getNombreCampo();
        switch (atributo.getTipo()) {
            case NUMERO -> {
                BigDecimal n = comoNumero(valor);
                if (n == null || n.stripTrailingZeros().scale() > 0) {
                    problemas.add("\"" + campo + "\" espera un número entero");
                }
            }
            case DECIMAL -> {
                if (comoNumero(valor) == null) {
                    problemas.add("\"" + campo + "\" espera un número");
                }
            }
            case FECHA -> {
                if (!esFecha(valor)) {
                    problemas.add("\"" + campo + "\" espera una fecha (AAAA-MM-DD)");
                }
            }
            case BOOLEANO -> {
                if (!(valor instanceof Boolean) && !"true".equals(valor) && !"false".equals(valor)) {
                    problemas.add("\"" + campo + "\" espera sí/no");
                }
            }
            case LISTA -> {
                if (!opciones(atributo).contains(String.valueOf(valor))) {
                    problemas.add("\"" + campo + "\" tiene que ser una de: " + opciones(atributo));
                }
            }
            case MULTILISTA -> {
                if (!(valor instanceof List<?> lista) || !opciones(atributo)
                        .containsAll(lista.stream().map(String::valueOf).toList())) {
                    problemas.add("\"" + campo + "\" tiene que ser una lista de: " + opciones(atributo));
                }
            }
            default -> { /* TEXTO: cualquier string vale */ }
        }
    }

    private static BigDecimal comoNumero(Object valor) {
        if (valor instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(String.valueOf(valor).trim());
        } catch (NumberFormatException noEsNumero) {
            return null;
        }
    }

    private static boolean esFecha(Object valor) {
        if (valor instanceof LocalDate) {
            return true;
        }
        try {
            LocalDate.parse(String.valueOf(valor));
            return true;
        } catch (DateTimeParseException noEsFecha) {
            return false;
        }
    }

    private static List<String> opciones(AtributoTipoRecurso atributo) {
        return atributo.getOpciones() == null ? List.of() : atributo.getOpciones();
    }
}
