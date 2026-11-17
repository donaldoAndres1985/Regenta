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
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;

/**
 * HU-147 · la tarifa de IVA probada contra PostgreSQL, con dos negocios (CLAUDE.md §11).
 */
@DataJpaTest
class TarifaDeIvaEnLaBaseTest extends PostgresDePrueba {

    /** Ferretería Central: los productos que llegaron desde Sara (V5/V6). */
    private static final UUID FERRETERIA = UUID.fromString("c1000000-0000-4000-8000-000000000001");

    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-147 C5 · Los productos que existían antes de la migración quedan en IVA_19, «por revisar», con el mismo precio")
    void losMigradosQuedanEn19PorRevisar() {
        comoElNegocio(jdbc, FERRETERIA);

        Integer total = jdbc.queryForObject("select count(*) from inventario.productos", Integer.class);
        Integer distintos = jdbc.queryForObject(
                "select count(*) from inventario.productos where tarifa_iva <> 'IVA_19' or not tarifa_por_revisar",
                Integer.class);
        BigDecimal barniz = jdbc.queryForObject(
                "select precio_venta from inventario.productos where sku = '7707031403512'", BigDecimal.class);

        assertThat(total).isGreaterThan(0);
        assertThat(distintos).isZero();
        assertThat(barniz).isEqualByComparingTo("35500");
    }

    @Test
    @DisplayName("HU-147 I1 · Una fila sin tarifa no entra a la base")
    void laBaseExigeUnaTarifa() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Producto base = guardar(negocio, "BASE-1", TarifaIva.IVA_19);

        assertThatThrownBy(() -> jdbc.update(
                "update inventario.productos set tarifa_iva = null where id = ?", base.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-147 I1 · Una tarifa fuera de la lista no entra a la base")
    void laBaseExigeUnaTarifaDeLaLista() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Producto base = guardar(negocio, "BASE-2", TarifaIva.IVA_19);

        assertThatThrownBy(() -> jdbc.update(
                "update inventario.productos set tarifa_iva = 'IVA_8' where id = ?", base.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-147 C6 · Dados dos negocios, B no ve las tarifas ni las marcas de revisión de A")
    void cadaNegocioVeSoloSusTarifas() {
        UUID negocioA = UUID.randomUUID();
        UUID negocioB = UUID.randomUUID();
        comoElNegocio(jdbc, negocioA);
        Producto deA = guardar(negocioA, "A-1", TarifaIva.IVA_5);
        em.flush();
        em.clear();

        comoElNegocio(jdbc, negocioB);
        Integer visibles = jdbc.queryForObject(
                "select count(*) from inventario.productos where id = ?", Integer.class, deA.getId());

        assertThat(visibles).isZero();
        assertThat(productos.findById(deA.getId())).isEmpty();

        comoElNegocio(jdbc, negocioA);
        assertThat(productos.findById(deA.getId())).get()
                .extracting(Producto::getTarifaIva).isEqualTo(TarifaIva.IVA_5);
    }

    private Producto guardar(UUID negocio, String sku, TarifaIva tarifa) {
        UUID categoria = UUID.randomUUID();
        UUID unidad = UUID.randomUUID();
        jdbc.update("insert into inventario.categorias (id, negocio_id, nombre) values (?, ?, ?)",
                categoria, negocio, "Pinturas " + sku);
        jdbc.update("insert into inventario.unidades_medida (id, negocio_id, codigo, nombre) values (?, ?, ?, ?)",
                unidad, negocio, "U" + sku, "Unidad");
        Producto p = Producto.nuevo(negocio, sku, "Producto " + sku, categoria, unidad, tarifa,
                UUID.randomUUID());
        p.datosBasicos(null, null, null, new BigDecimal("35500"), null);
        return productos.saveAndFlush(p);
    }
}
