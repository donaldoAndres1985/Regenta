package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;

/** HU-152 · la ubicación por producto y bodega, contra PostgreSQL con dos negocios. */
@DataJpaTest
class UbicacionEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private ExistenciaRepositorio existencias;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID bodega(UUID negocio, String codigo) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into inventario.bodegas (id, negocio_id, codigo, nombre) values (?, ?, ?, ?)",
                id, negocio, codigo, "Bodega " + codigo);
        return id;
    }

    private Producto producto(UUID negocio) {
        UUID categoria = UUID.randomUUID();
        UUID unidad = UUID.randomUUID();
        jdbc.update("insert into inventario.categorias (id, negocio_id, nombre) values (?, ?, ?)",
                categoria, negocio, "Pinturas");
        jdbc.update("insert into inventario.unidades_medida (id, negocio_id, codigo, nombre) values (?, ?, ?, ?)",
                unidad, negocio, "GLN", "Galón");
        Producto p = Producto.nuevo(negocio, "BAR-1", "Barniz", categoria, unidad, TarifaIva.IVA_19,
                UUID.randomUUID());
        p.datosBasicos(null, null, null, new BigDecimal("35500"), null);
        return productos.saveAndFlush(p);
    }

    @Test
    @DisplayName("HU-152 C1 · Cada bodega guarda su propia ubicación del mismo producto")
    void cadaBodegaLaSuya() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Producto p = producto(negocio);
        UUID principal = bodega(negocio, "PRI");
        UUID otra = bodega(negocio, "B2");
        Existencia a = Existencia.enCero(negocio, p.getId(), principal);
        a.fijarUbicacion("Pasillo 3 – E2");
        Existencia b = Existencia.enCero(negocio, p.getId(), otra);
        b.fijarUbicacion("Estiba 5");
        existencias.saveAndFlush(a);
        existencias.saveAndFlush(b);
        em.clear();

        assertThat(existencias.findByProductoIdAndBodegaId(p.getId(), principal)).get()
                .extracting(Existencia::getUbicacion).isEqualTo("Pasillo 3 – E2");
        assertThat(existencias.findByProductoIdAndBodegaId(p.getId(), otra)).get()
                .extracting(Existencia::getUbicacion).isEqualTo("Estiba 5");
    }

    @Test
    @DisplayName("HU-152 C4 · Dados dos negocios, B no ve las ubicaciones de A")
    void cadaNegocioVeLasSuyas() {
        UUID negocioA = UUID.randomUUID();
        comoElNegocio(jdbc, negocioA);
        Producto p = producto(negocioA);
        UUID bodegaA = bodega(negocioA, "PRI");
        Existencia e = Existencia.enCero(negocioA, p.getId(), bodegaA);
        e.fijarUbicacion("Pasillo 1");
        existencias.saveAndFlush(e);
        em.clear();

        comoElNegocio(jdbc, UUID.randomUUID());
        Integer visibles = jdbc.queryForObject(
                "select count(*) from inventario.existencias where ubicacion is not null and producto_id = ?",
                Integer.class, p.getId());

        assertThat(visibles).isZero();
    }
}
