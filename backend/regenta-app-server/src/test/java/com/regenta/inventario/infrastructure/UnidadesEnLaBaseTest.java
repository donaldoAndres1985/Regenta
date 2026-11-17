package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.domain.UnidadMedida;

/** HU-155 C4 · las unidades viven por negocio, contra PostgreSQL con dos negocios. */
@DataJpaTest
class UnidadesEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-155 C4 · Dos negocios pueden tener cada uno su GLN y ninguno ve la del otro")
    void cadaNegocioLasSuyas() {
        UUID negocioA = UUID.randomUUID();
        UUID negocioB = UUID.randomUUID();
        comoElNegocio(jdbc, negocioA);
        unidades.saveAndFlush(UnidadMedida.nueva(negocioA, "GLN", "Galón A", true));
        em.clear();

        comoElNegocio(jdbc, negocioB);
        unidades.saveAndFlush(UnidadMedida.nueva(negocioB, "GLN", "Galón B", true));
        em.clear();

        assertThat(unidades.findByNegocioIdOrderByCodigo(negocioB)).extracting(UnidadMedida::getNombre)
                .containsExactly("Galón B");
        assertThat(unidades.findByNegocioIdOrderByCodigo(negocioA)).isEmpty();
    }
}
