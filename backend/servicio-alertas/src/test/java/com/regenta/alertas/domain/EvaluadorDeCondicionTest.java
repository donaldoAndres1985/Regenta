package com.regenta.alertas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-092: la condición declarativa, sin Spring. */
class EvaluadorDeCondicionTest {

    @Test
    @DisplayName("Condición vacía siempre coincide")
    void vaciaCoincide() {
        assertThat(EvaluadorDeCondicion.coincide(Map.of(), Map.of("x", 1))).isTrue();
    }

    @Test
    @DisplayName("Comparación numérica con <=")
    void numericoMenorIgual() {
        Map<String, Object> cond = Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 30);
        assertThat(EvaluadorDeCondicion.coincide(cond, Map.of("dias_para_vencer", 20))).isTrue();
        assertThat(EvaluadorDeCondicion.coincide(cond, Map.of("dias_para_vencer", 40))).isFalse();
        assertThat(EvaluadorDeCondicion.coincide(cond, Map.of("dias_para_vencer", 30))).isTrue();
    }

    @Test
    @DisplayName("Campo ausente no coincide")
    void campoAusente() {
        Map<String, Object> cond = Map.of("campo", "stock", "op", "<", "valor", 5);
        assertThat(EvaluadorDeCondicion.coincide(cond, Map.of("otra", 1))).isFalse();
    }

    @Test
    @DisplayName("todas = Y, alguna = O")
    void compuestas() {
        Map<String, Object> todas = Map.of("todas", List.of(
                Map.of("campo", "a", "op", ">=", "valor", 10),
                Map.of("campo", "b", "op", "==", "valor", "X")));
        assertThat(EvaluadorDeCondicion.coincide(todas, Map.of("a", 12, "b", "X"))).isTrue();
        assertThat(EvaluadorDeCondicion.coincide(todas, Map.of("a", 12, "b", "Y"))).isFalse();

        Map<String, Object> alguna = Map.of("alguna", List.of(
                Map.of("campo", "a", "op", ">", "valor", 100),
                Map.of("campo", "b", "op", "==", "valor", "X")));
        assertThat(EvaluadorDeCondicion.coincide(alguna, Map.of("a", 1, "b", "X"))).isTrue();
        assertThat(EvaluadorDeCondicion.coincide(alguna, Map.of("a", 1, "b", "Z"))).isFalse();
    }
}
