package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.domain.EquivalenciaDeDetallado;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;

/** HU-161 · equivalencias contra PostgreSQL, con dos negocios (I16, I21). */
@DataJpaTest
class EquivalenciasEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private EquivalenciaDeDetalladoRepositorio equivalencias;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    private Producto[] cuneteYGalon(UUID negocio) {
        UUID categoria = UUID.randomUUID();
        UUID unidad = UUID.randomUUID();
        jdbc.update("insert into inventario.categorias (id, negocio_id, nombre) values (?, ?, 'Pinturas')", categoria, negocio);
        jdbc.update("insert into inventario.unidades_medida (id, negocio_id, codigo, nombre) values (?, ?, 'GLN', 'Galón')", unidad, negocio);
        Producto cunete = Producto.nuevo(negocio, "CUN-1", "Pintulux blanco cuñete", categoria, unidad, TarifaIva.IVA_19, UUID.randomUUID());
        Producto galon = Producto.nuevo(negocio, "GAL-1", "Pintulux blanco galón", categoria, unidad, TarifaIva.IVA_19, UUID.randomUUID());
        return new Producto[] { productos.saveAndFlush(cunete), productos.saveAndFlush(galon) };
    }

    @Test
    @DisplayName("HU-161 C4 · La base no deja una segunda equivalencia para el mismo par")
    void unaPorPar() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Producto[] p = cuneteYGalon(negocio);
        equivalencias.saveAndFlush(EquivalenciaDeDetallado.entre(p[0], p[1], new BigDecimal("5")));

        assertThatThrownBy(() -> equivalencias.saveAndFlush(
                EquivalenciaDeDetallado.entre(p[0], p[1], new BigDecimal("4"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-161 · Dados dos negocios, B no ve las equivalencias de A")
    void cadaNegocioLasSuyas() {
        UUID negocioA = UUID.randomUUID();
        comoElNegocio(jdbc, negocioA);
        Producto[] p = cuneteYGalon(negocioA);
        EquivalenciaDeDetallado e = equivalencias.saveAndFlush(
                EquivalenciaDeDetallado.entre(p[0], p[1], new BigDecimal("5")));
        em.clear();

        comoElNegocio(jdbc, UUID.randomUUID());
        assertThat(equivalencias.findById(e.getId())).isEmpty();
    }
}
