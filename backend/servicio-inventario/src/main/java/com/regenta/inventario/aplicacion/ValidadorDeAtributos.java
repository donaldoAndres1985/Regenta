package com.regenta.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.domain.AtributoCategoria;

/**
 * Valida el mapa de {@code atributos} de un producto contra lo que exige su
 * categoria (HU-028). PostgreSQL no comprueba la forma del JSON: esto es lo que
 * hace que "Medicamentos exige lote y vencimiento" sea cierto sin programar una
 * columna.
 */
@Component
public class ValidadorDeAtributos {

    /**
     * Devuelve el mapa listo para guardar (mismos valores, normalizados) o
     * lanza 422 nombrando los campos que fallan.
     */
    public Map<String, Object> validar(List<AtributoCategoria> definiciones,
            Map<String, Object> valores) {
        Map<String, Object> entrada = valores == null ? Map.of() : valores;
        Map<String, Object> salida = new LinkedHashMap<>(entrada);
        List<String> problemas = new ArrayList<>();

        for (AtributoCategoria atributo : definiciones) {
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
                    "Los atributos de la categoria no cuadran: " + String.join("; ", problemas));
        }
        return salida;
    }

    private void validarValor(AtributoCategoria atributo, Object valor, List<String> problemas) {
        String campo = atributo.getNombreCampo();
        switch (atributo.getTipo()) {
            case NUMERO -> {
                BigDecimal numero = comoNumero(valor);
                if (numero == null || numero.stripTrailingZeros().scale() > 0) {
                    problemas.add("\"" + campo + "\" espera un numero entero");
                    return;
                }
                validarRango(atributo, numero, problemas);
            }
            case DECIMAL -> {
                BigDecimal numero = comoNumero(valor);
                if (numero == null) {
                    problemas.add("\"" + campo + "\" espera un numero");
                    return;
                }
                validarRango(atributo, numero, problemas);
            }
            case FECHA -> {
                if (!esFecha(valor)) {
                    problemas.add("\"" + campo + "\" espera una fecha (AAAA-MM-DD)");
                }
            }
            case BOOLEANO -> {
                if (!(valor instanceof Boolean) && !"true".equals(valor) && !"false".equals(valor)) {
                    problemas.add("\"" + campo + "\" espera si/no");
                }
            }
            case LISTA -> {
                if (!opciones(atributo).contains(String.valueOf(valor))) {
                    problemas.add("\"" + campo + "\" tiene que ser una de: " + opciones(atributo));
                }
            }
            case MULTILISTA -> {
                if (!(valor instanceof List<?> lista)
                        || !opciones(atributo).containsAll(lista.stream().map(String::valueOf).toList())) {
                    problemas.add("\"" + campo + "\" tiene que ser una lista de: " + opciones(atributo));
                }
            }
            case TEXTO -> {
                String regex = atributo.getValidacionRegex();
                if (regex != null && !regex.isBlank()
                        && !Pattern.matches(regex, String.valueOf(valor))) {
                    problemas.add("\"" + campo + "\" no cumple el formato exigido");
                }
            }
            default -> { }
        }
    }

    private void validarRango(AtributoCategoria atributo, BigDecimal numero, List<String> problemas) {
        if (atributo.getValorMin() != null && numero.compareTo(atributo.getValorMin()) < 0) {
            problemas.add("\"" + atributo.getNombreCampo() + "\" no puede ser menor que "
                    + atributo.getValorMin());
        }
        if (atributo.getValorMax() != null && numero.compareTo(atributo.getValorMax()) > 0) {
            problemas.add("\"" + atributo.getNombreCampo() + "\" no puede ser mayor que "
                    + atributo.getValorMax());
        }
    }

    private static BigDecimal comoNumero(Object valor) {
        if (valor instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(String.valueOf(valor).trim());
        } catch (NumberFormatException e) {
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
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static List<String> opciones(AtributoCategoria atributo) {
        return atributo.getOpciones() == null ? List.of() : atributo.getOpciones();
    }
}
