package com.regenta.caja.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.MetodoPagoCaja;

/**
 * HU-139 · las garantías de {@code caja.config_caja} probadas contra PostgreSQL, con dos
 * negocios cargados (CLAUDE.md §11).
 */
@DataJpaTest
class ConfigDeCajaEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ConfigDeCajaRepositorio repositorio;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();

    @Test
    @DisplayName("HU-139 C1 · Dada una fila nueva sin la columna, la base pone por defecto TARJETA_DEBITO, TARJETA_CREDITO, BONO y CREDITO")
    void laBasePoneElValorPorDefecto() {
        comoElNegocio(jdbc, negocioA);
        jdbc.update("insert into caja.config_caja (negocio_id) values (?)", negocioA);

        String metodos = jdbc.queryForObject(
                "select array_to_string(metodos_no_cuadrables, ',') from caja.config_caja where negocio_id = ?",
                String.class, negocioA);

        assertThat(metodos.split(",")).containsExactlyInAnyOrder(
                "TARJETA_DEBITO", "TARJETA_CREDITO", "BONO", "CREDITO");
    }

    @Test
    @DisplayName("HU-139 C2 · Dado un cambio guardado, cuando se vuelve a leer de la base, devuelve la lista nueva")
    void laListaSobreviveAlViajeALaBase() {
        comoElNegocio(jdbc, negocioA);
        ConfigDeCaja config = ConfigDeCaja.porDefecto(negocioA);
        config.fijarMetodosNoCuadrables(EnumSet.of(MetodoPagoCaja.QR));
        repositorio.save(config);
        em.flush();
        em.clear();

        ConfigDeCaja leida = repositorio.findById(negocioA).orElseThrow();

        assertThat(leida.getMetodosNoCuadrables()).containsExactly(MetodoPagoCaja.QR);
    }

    @Test
    @DisplayName("HU-139 C4 · Dada una fila con EFECTIVO como no cuadrable, la base la rechaza aunque la app falle")
    void laBaseRechazaElEfectivoComoNoCuadrable() {
        comoElNegocio(jdbc, negocioA);

        assertThatThrownBy(() -> jdbc.update(
                "insert into caja.config_caja (negocio_id, metodos_no_cuadrables) values (?, ARRAY['EFECTIVO','BONO'])",
                negocioA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-139 C6 · Dados dos negocios, cuando A cambia su lista, la de B no cambia y B no puede leer la de A")
    void cadaNegocioVeSoloLaSuya() {
        comoElNegocio(jdbc, negocioA);
        ConfigDeCaja deA = ConfigDeCaja.porDefecto(negocioA);
        deA.fijarMetodosNoCuadrables(EnumSet.of(MetodoPagoCaja.QR));
        repositorio.save(deA);
        em.flush(); // el INSERT tiene que salir mientras el contexto es A, o la RLS lo rechaza

        comoElNegocio(jdbc, negocioB);
        repositorio.save(ConfigDeCaja.porDefecto(negocioB));
        em.flush();
        em.clear();

        assertThat(repositorio.findById(negocioA)).isEmpty();
        assertThat(repositorio.findById(negocioB).orElseThrow().getMetodosNoCuadrables())
                .containsExactlyInAnyOrder(MetodoPagoCaja.TARJETA_DEBITO, MetodoPagoCaja.TARJETA_CREDITO,
                        MetodoPagoCaja.BONO, MetodoPagoCaja.CREDITO);

        comoElNegocio(jdbc, negocioA);
        assertThat(repositorio.findById(negocioA).orElseThrow().getMetodosNoCuadrables())
                .containsExactly(MetodoPagoCaja.QR);
    }
}
