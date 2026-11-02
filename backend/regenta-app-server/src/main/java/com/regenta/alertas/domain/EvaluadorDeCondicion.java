package com.regenta.alertas.domain;

import java.util.List;
import java.util.Map;

/**
 * Evalúa la condición declarativa de una regla contra los campos de un hecho
 * (HU-092). La condición es JSON, no código:
 *
 * <pre>
 *   {}                                              -> siempre cierto
 *   {"campo":"dias_para_vencer","op":"&lt;=","valor":30}
 *   {"todas":[ {...}, {...} ]}                       -> Y
 *   {"alguna":[ {...}, {...} ]}                      -> O
 * </pre>
 *
 * Un campo ausente en el hecho hace fallar la comparación (no coincide).
 */
public final class EvaluadorDeCondicion {

    private EvaluadorDeCondicion() {
    }

    @SuppressWarnings("unchecked")
    public static boolean coincide(Map<String, Object> condicion, Map<String, Object> campos) {
        if (condicion == null || condicion.isEmpty()) {
            return true;
        }
        if (condicion.containsKey("todas")) {
            return sublista(condicion.get("todas")).stream()
                    .allMatch(c -> coincide(c, campos));
        }
        if (condicion.containsKey("alguna")) {
            return sublista(condicion.get("alguna")).stream()
                    .anyMatch(c -> coincide(c, campos));
        }
        Object campo = condicion.get("campo");
        Object op = condicion.getOrDefault("op", "==");
        Object esperado = condicion.get("valor");
        if (campo == null) {
            return false;
        }
        if (!campos.containsKey(campo.toString())) {
            return false;
        }
        return comparar(campos.get(campo.toString()), op.toString(), esperado);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> sublista(Object crudo) {
        if (crudo instanceof List<?> lista) {
            return lista.stream()
                    .filter(e -> e instanceof Map)
                    .map(e -> (Map<String, Object>) e)
                    .toList();
        }
        return List.of();
    }

    private static boolean comparar(Object actual, String op, Object esperado) {
        Double a = aNumero(actual);
        Double b = aNumero(esperado);
        if (a != null && b != null) {
            int cmp = Double.compare(a, b);
            return switch (op) {
                case "==", "=" -> cmp == 0;
                case "!=", "<>" -> cmp != 0;
                case "<" -> cmp < 0;
                case "<=" -> cmp <= 0;
                case ">" -> cmp > 0;
                case ">=" -> cmp >= 0;
                default -> false;
            };
        }
        String sa = String.valueOf(actual);
        String sb = String.valueOf(esperado);
        return switch (op) {
            case "==", "=" -> sa.equals(sb);
            case "!=", "<>" -> !sa.equals(sb);
            case "contiene" -> sa.toLowerCase().contains(sb.toLowerCase());
            default -> false;
        };
    }

    private static Double aNumero(Object v) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        if (v instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException noEsNumero) {
            return null;
        }
    }
}
