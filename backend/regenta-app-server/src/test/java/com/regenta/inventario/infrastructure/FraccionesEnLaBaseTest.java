package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.domain.TablaDeFracciones;

/** HU-156 · las tablas de fracciones contra PostgreSQL, con dos negocios. */
@DataJpaTest
class FraccionesEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private TablaDeFraccionesRepositorio tablas;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    private TablaDeFracciones pintulux(UUID negocio) {
        return TablaDeFracciones.nueva(negocio, "Esmalte Pintulux galón", List.of(
                new TablaDeFracciones.Definicion("½ galón", new BigDecimal("0.5"), new BigDecimal("34000")),
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("18000"))));
    }

    @Test
    @DisplayName("HU-156 C1 · La tabla y sus fracciones sobreviven el viaje a la base, con sus precios")
    void viajeALaBase() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        TablaDeFracciones t = tablas.saveAndFlush(pintulux(negocio));
        em.clear();

        TablaDeFracciones leida = tablas.findById(t.getId()).orElseThrow();
        assertThat(leida.precioDe("¼ galón")).isEqualByComparingTo("18000");
        assertThat(leida.getFracciones()).hasSize(2);
    }

    @Test
    @DisplayName("HU-156 C4 · Cambiar el precio de una medida que ya existe se guarda sin chocar con la unicidad")
    void reemplazarConservaLasMedidas() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        TablaDeFracciones t = tablas.saveAndFlush(pintulux(negocio));
        t.reemplazarFracciones(List.of(
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("19000")),
                new TablaDeFracciones.Definicion("½ galón", new BigDecimal("0.5"), new BigDecimal("35000"))));
        tablas.saveAndFlush(t);
        em.clear();

        assertThat(tablas.findById(t.getId()).orElseThrow().precioDe("¼ galón")).isEqualByComparingTo("19000");
    }

    @Test
    @DisplayName("HU-156 I12 · La base rechaza un factor fuera de (0, 1) aunque la app falle")
    void laBaseRechazaElFactor() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        TablaDeFracciones t = tablas.saveAndFlush(pintulux(negocio));

        assertThatThrownBy(() -> jdbc.update(
                "insert into inventario.fracciones (id, negocio_id, tabla_id, medida, factor, precio) values (?, ?, ?, 'x', 1.5, 1000)",
                UUID.randomUUID(), negocio, t.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-156 C6 · Dados dos negocios, B no ve las tablas de A")
    void cadaNegocioLasSuyas() {
        UUID negocioA = UUID.randomUUID();
        comoElNegocio(jdbc, negocioA);
        TablaDeFracciones t = tablas.saveAndFlush(pintulux(negocioA));
        em.clear();

        comoElNegocio(jdbc, UUID.randomUUID());
        assertThat(tablas.findById(t.getId())).isEmpty();
        Integer fracciones = jdbc.queryForObject("select count(*) from inventario.fracciones where tabla_id = ?",
                Integer.class, t.getId());
        assertThat(fracciones).isZero();
    }
}
