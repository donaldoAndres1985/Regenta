package com.regenta.compras.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-165 · El lote y el vencimiento que exige un producto al recibirlo (I16 de Compras). */
class RecepcionLineaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 20);

    private RecepcionLinea linea(boolean manejaLotes, boolean perecedero, String lote, LocalDate vence) {
        return RecepcionLinea.nueva(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), BigDecimal.TEN, new BigDecimal("10500"), manejaLotes, perecedero,
                HOY, lote, vence, "INVIMA-1");
    }

    @Test
    @DisplayName("HU-165 C6 · Un producto que maneja lotes, recibido sin lote, se rechaza (422)")
    void manejaLotesSinLote() {
        assertThatThrownBy(() -> linea(true, false, "  ", null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("maneja lotes");
    }

    @Test
    @DisplayName("HU-165 C6 · Un perecedero sin fecha de vencimiento se rechaza (422)")
    void perecederoSinVencimiento() {
        assertThatThrownBy(() -> linea(true, true, "L-1", null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("perecedero");
    }

    @Test
    @DisplayName("HU-165 C7 · Un lote vencido antes de la fecha de recepción se rechaza (422)")
    void loteVencido() {
        assertThatThrownBy(() -> linea(true, true, "L-1", HOY.minusDays(1)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("venció");
    }

    @Test
    @DisplayName("HU-165 C7 · Un lote que vence el mismo día de la recepción se acepta")
    void venceHoy() {
        assertThat(linea(true, true, "L-1", HOY).getFechaVencimiento()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("HU-165 · Un producto con lotes pero no perecedero se recibe con lote y sin vencimiento")
    void loteSinVencimiento() {
        RecepcionLinea l = linea(true, false, " L-9 ", null);

        assertThat(l.getCodigoLote()).isEqualTo("L-9");
        assertThat(l.getFechaVencimiento()).isNull();
    }

    @Test
    @DisplayName("HU-048 C2 · Si el producto no maneja lotes, los datos de lote que vengan no se guardan")
    void sinLotesNoGuarda() {
        RecepcionLinea l = linea(false, false, "L-1", HOY.minusYears(1));

        assertThat(l.getCodigoLote()).isNull();
        assertThat(l.getFechaVencimiento()).isNull();
        assertThat(l.getRegistroSanitario()).isNull();
    }
}
