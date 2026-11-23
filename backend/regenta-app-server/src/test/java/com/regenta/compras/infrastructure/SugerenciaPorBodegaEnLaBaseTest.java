package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.EstadoSugerencia;
import com.regenta.compras.domain.SugerenciaDeCompra;

/** HU-181 · I30 contra PostgreSQL (compras V20): una sugerencia pendiente por producto y bodega. */
@DataJpaTest
class SugerenciaPorBodegaEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private SugerenciaDeCompraRepositorio sugerencias;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();

    private SugerenciaDeCompra nueva(UUID bodega) {
        return SugerenciaDeCompra.desdeStockBajo(negocio, producto, "Acetaminofén 500 mg", bodega,
                new BigDecimal("8"), BigDecimal.TEN, new BigDecimal("50"));
    }

    @Test
    @DisplayName("C3 · Dos bodegas, dos pendientes; la misma bodega dos veces, no")
    void unaPorBodega() {
        comoElNegocio(jdbc, negocio);
        UUID principal = UUID.randomUUID();
        sugerencias.saveAndFlush(nueva(principal));
        sugerencias.saveAndFlush(nueva(UUID.randomUUID()));

        assertThat(sugerencias.findByNegocioIdAndProductoIdAndEstado(negocio, producto, EstadoSugerencia.PENDIENTE))
                .hasSize(2);
        assertThat(sugerencias.findByNegocioIdAndProductoIdAndBodegaIdAndEstado(negocio, producto, principal,
                EstadoSugerencia.PENDIENTE)).isPresent();
        assertThatThrownBy(() -> sugerencias.saveAndFlush(nueva(principal)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Sin bodega también es una sola, y se encuentra pidiendo la bodega nula")
    void sinBodega() {
        comoElNegocio(jdbc, negocio);
        sugerencias.saveAndFlush(nueva(null));

        assertThat(sugerencias.findByNegocioIdAndProductoIdAndBodegaIdAndEstado(negocio, producto, null,
                EstadoSugerencia.PENDIENTE)).isPresent();
        assertThatThrownBy(() -> sugerencias.saveAndFlush(nueva(null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Una descartada no estorba a la nueva pendiente de la misma bodega")
    void laDescartadaNoCuenta() {
        comoElNegocio(jdbc, negocio);
        UUID bodega = UUID.randomUUID();
        SugerenciaDeCompra vieja = sugerencias.saveAndFlush(nueva(bodega));
        vieja.descartar();
        sugerencias.saveAndFlush(vieja);

        sugerencias.saveAndFlush(nueva(bodega));

        assertThat(sugerencias.findByNegocioIdAndProductoIdAndEstado(negocio, producto, EstadoSugerencia.PENDIENTE))
                .hasSize(1);
    }
}
