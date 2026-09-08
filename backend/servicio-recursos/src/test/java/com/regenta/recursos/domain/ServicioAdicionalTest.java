package com.regenta.recursos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-068. El modo de cobro decide cuántas unidades de un servicio adicional se cobran. */
class ServicioAdicionalTest {

    private static final UUID NEGOCIO = UUID.randomUUID();

    private static ServicioAdicional servicio(ModoCobro modo, String precio, UUID productoId) {
        return ServicioAdicional.crear(NEGOCIO, "SVC", "Desayuno", null, new BigDecimal(precio),
                null, modo, productoId);
    }

    @Test
    @DisplayName("Criterio 1: por persona y noche, 2 personas × 3 noches = 6 unidades")
    void porPersonaNoche() {
        ServicioAdicional s = servicio(ModoCobro.POR_PERSONA_NOCHE, "15000", null);

        assertThat(s.unidadesPara(2, 3, 0)).isEqualTo(6);
        assertThat(s.subtotalPara(2, 3, 0)).isEqualByComparingTo("90000");
    }

    @Test
    @DisplayName("Cada modo de cobro cuenta sus unidades")
    void cadaModoCuentaLoSuyo() {
        assertThat(servicio(ModoCobro.POR_ESTANCIA, "10", null).unidadesPara(4, 5, 9)).isEqualTo(1);
        assertThat(servicio(ModoCobro.POR_NOCHE, "10", null).unidadesPara(4, 5, 9)).isEqualTo(5);
        assertThat(servicio(ModoCobro.POR_PERSONA, "10", null).unidadesPara(4, 5, 9)).isEqualTo(4);
        assertThat(servicio(ModoCobro.POR_PERSONA_NOCHE, "10", null).unidadesPara(4, 5, 9))
                .isEqualTo(20);
        assertThat(servicio(ModoCobro.POR_UNIDAD, "10", null).unidadesPara(4, 5, 9)).isEqualTo(9);
    }

    @Test
    @DisplayName("Un servicio con producto descuenta inventario; sin producto, no")
    void descuentaInventarioSegunProducto() {
        assertThat(servicio(ModoCobro.POR_UNIDAD, "10", UUID.randomUUID()).descuentaInventario())
                .isTrue();
        assertThat(servicio(ModoCobro.POR_UNIDAD, "10", null).descuentaInventario()).isFalse();
    }

    @Test
    @DisplayName("Un precio negativo se guarda como cero")
    void precioNegativoEsCero() {
        assertThat(servicio(ModoCobro.POR_ESTANCIA, "-5", null).getPrecio())
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Un modo de cobro desconocido se rechaza")
    void modoDesconocido() {
        assertThatThrownBy(() -> ModoCobro.desde("POR_CAPRICHO"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
