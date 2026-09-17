package com.regenta.comandas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-089. Reglas puras de una cuenta: su reparto de líneas y su cobro. */
class CuentaTest {

    private static ComandaLinea linea(BigDecimal subtotal) {
        ComandaLinea l = ComandaLinea.crear(UUID.randomUUID(), UUID.randomUUID(), (short) 1,
                UUID.randomUUID(), "Bandeja paisa", subtotal, UUID.randomUUID(), CursoDeComanda.FUERTE,
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, 1);
        return l;
    }

    @Test
    @DisplayName("Nace ABIERTA, en ceros, y admite líneas")
    void nace() {
        Cuenta c = Cuenta.crear(UUID.randomUUID(), UUID.randomUUID(), (short) 1, " Camilo ", null);
        assertThat(c.getEstado()).isEqualTo(EstadoDeCuenta.ABIERTA);
        assertThat(c.getEtiqueta()).isEqualTo("Camilo");
        assertThat(c.getModoDivision()).isEqualTo(ModoDeDivision.POR_ITEM);
        assertThat(c.getTotal()).isEqualByComparingTo("0");
        assertThat(c.admiteLineas()).isTrue();
    }

    @Test
    @DisplayName("Criterio 2: recalcular suma el subtotal y el impuesto según la proporción de cada línea")
    void recalcular() {
        Cuenta c = Cuenta.crear(UUID.randomUUID(), UUID.randomUUID(), (short) 1, null, null);
        ComandaLinea l = linea(new BigDecimal("32000")); // sin impuesto en este fixture
        CuentaLinea cl = CuentaLinea.de(c.getNegocioId(), c.getId(), l.getId());
        cl.fijarReparto(new BigDecimal("0.5"), l.getTotal().multiply(new BigDecimal("0.5")));

        c.recalcular(List.of(cl), Map.of(l.getId(), l));

        assertThat(c.getSubtotal()).isEqualByComparingTo("16000");
        assertThat(c.getTotal()).isEqualByComparingTo("16000");
    }

    @Test
    @DisplayName("Criterio 5: marcar pagada deja el estado PAGADA y el monto pagado igual al total")
    void marcarPagada() {
        Cuenta c = Cuenta.crear(UUID.randomUUID(), UUID.randomUUID(), (short) 1, null, null);
        ComandaLinea l = linea(new BigDecimal("40000"));
        CuentaLinea cl = CuentaLinea.de(c.getNegocioId(), c.getId(), l.getId());
        cl.fijarReparto(BigDecimal.ONE, l.getTotal());
        c.recalcular(List.of(cl), Map.of(l.getId(), l));

        c.marcarPagada();

        assertThat(c.getEstado()).isEqualTo(EstadoDeCuenta.PAGADA);
        assertThat(c.getPagado()).isEqualByComparingTo(c.getTotal());
        assertThat(c.admiteLineas()).isFalse();
    }

    @Test
    @DisplayName("Criterio 4: una cuenta ya pagada no se vuelve a pagar")
    void noSeRepagaNiSeReanula() {
        Cuenta c = Cuenta.crear(UUID.randomUUID(), UUID.randomUUID(), (short) 1, null, null);
        c.marcarPagada();
        assertThatThrownBy(c::marcarPagada).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("ModoDeDivision.desde cae a POR_ITEM si el texto no existe o viene vacío")
    void modoDeDivisionDesde() {
        assertThat(ModoDeDivision.desde(null)).isEqualTo(ModoDeDivision.POR_ITEM);
        assertThat(ModoDeDivision.desde("")).isEqualTo(ModoDeDivision.POR_ITEM);
        assertThat(ModoDeDivision.desde("no-existe")).isEqualTo(ModoDeDivision.POR_ITEM);
        assertThat(ModoDeDivision.desde("partes_iguales")).isEqualTo(ModoDeDivision.PARTES_IGUALES);
    }
}
