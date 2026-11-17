package com.regenta.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-156 · la tabla de fracciones y sus reglas (I12, I13). */
class TablaDeFraccionesTest {

    static List<TablaDeFracciones.Definicion> pintulux() {
        return List.of(
                new TablaDeFracciones.Definicion("½ galón", new BigDecimal("0.50"), new BigDecimal("34000")),
                new TablaDeFracciones.Definicion("¾ galón", new BigDecimal("0.75"), new BigDecimal("48000")),
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("18000")),
                new TablaDeFracciones.Definicion("⅛ galón", new BigDecimal("0.125"), new BigDecimal("10000")));
    }

    @Test
    @DisplayName("HU-156 C1 · La tabla guarda las fracciones con sus precios exactos, sin calcular proporcionales")
    void guardaLosPreciosEscritos() {
        TablaDeFracciones t = TablaDeFracciones.nueva(UUID.randomUUID(), "Esmalte Pintulux galón", pintulux());

        assertThat(t.getFracciones()).extracting(Fraccion::getMedida)
                .containsExactly("½ galón", "¾ galón", "¼ galón", "⅛ galón");
        assertThat(t.precioDe("½ galón")).isEqualByComparingTo("34000");
        assertThat(t.precioDe("¼ galón")).isEqualByComparingTo("18000");
    }

    @Test
    @DisplayName("HU-156 C2 · Un factor de 0, 1 o mayor que 1 se rechaza")
    void factorFueraDeRango() {
        for (String factor : new String[] { "0", "1", "1.5" }) {
            assertThatThrownBy(() -> TablaDeFracciones.nueva(UUID.randomUUID(), "Mala", List.of(
                    new TablaDeFracciones.Definicion("x", new BigDecimal(factor), new BigDecimal("1000")))))
                    .isInstanceOf(ReglaDeNegocioException.class);
        }
    }

    @Test
    @DisplayName("HU-156 C2 · Una medida repetida en la tabla se rechaza")
    void medidaRepetida() {
        assertThatThrownBy(() -> TablaDeFracciones.nueva(UUID.randomUUID(), "Mala", List.of(
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("18000")),
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("19000")))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("¼ galón");
    }

    @Test
    @DisplayName("HU-156 C4 · Cambiar el precio del ¼ en la tabla lo cambia para todos los que la usan")
    void cambiarUnPrecio() {
        TablaDeFracciones t = TablaDeFracciones.nueva(UUID.randomUUID(), "Esmalte Pintulux galón", pintulux());

        t.reemplazarFracciones(List.of(
                new TablaDeFracciones.Definicion("½ galón", new BigDecimal("0.50"), new BigDecimal("34000")),
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("19000"))));

        assertThat(t.precioDe("¼ galón")).isEqualByComparingTo("19000");
        assertThat(t.getFracciones()).hasSize(2);
    }
}
