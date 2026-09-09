package com.regenta.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-079. Reglas puras de una línea de receta: cantidad con merma y su costo. */
class RecetaTest {

    private static final UUID NEG = UUID.randomUUID();
    private static final UUID ITEM = UUID.randomUUID();
    private static final UUID PROD = UUID.randomUUID();

    private static Receta linea(String cantidad, String merma) {
        return Receta.crear(NEG, ITEM, PROD, "Carne", new BigDecimal(cantidad), "kg",
                new BigDecimal(merma), false);
    }

    @Test
    @DisplayName("La cantidad debe ser mayor que cero y la merma entre 0 y 1")
    void validaciones() {
        assertThatThrownBy(() -> linea("0", "0")).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> linea("-1", "0")).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> linea("1", "1")).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("cantidadConMerma añade la fracción de merma")
    void cantidadConMerma() {
        assertThat(linea("0.25", "0.10").cantidadConMerma()).isEqualByComparingTo("0.275000");
        assertThat(linea("0.2", "0").cantidadConMerma()).isEqualByComparingTo("0.2");
    }

    @Test
    @DisplayName("El costo de la línea es la cantidad con merma por el costo unitario del insumo")
    void costoDeLaLinea() {
        Receta r = linea("0.25", "0.10");
        assertThat(r.costoCon(new BigDecimal("40000"))).isEqualByComparingTo("11000");
        assertThat(r.costoCon(null)).isEqualByComparingTo("0");
        assertThat(r.costoCon(BigDecimal.ZERO)).isEqualByComparingTo("0");
    }
}
