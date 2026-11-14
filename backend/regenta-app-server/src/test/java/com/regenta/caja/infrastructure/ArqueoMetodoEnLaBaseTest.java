package com.regenta.caja.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.LineaDeArqueo;
import com.regenta.caja.domain.MetodoPagoCaja;

/**
 * HU-140 · las garantías de {@code caja.arqueo_metodos} probadas contra PostgreSQL, con dos
 * negocios cargados (CLAUDE.md §11).
 */
@DataJpaTest
class ArqueoMetodoEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ArqueoMetodoRepositorio repositorio;
    @Autowired
    private TestEntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private UUID sesionA;
    private UUID sesionB;

    @BeforeEach
    void dosNegociosConUnaSesionCadaUno() {
        sesionA = sesionDe(negocioA);
        sesionB = sesionDe(negocioB);
    }

    @Test
    @DisplayName("HU-140 C6 · Dado el cierre, la diferencia de cada línea es contado − esperado y la calcula la base")
    void laDiferenciaLaCalculaLaBase() {
        comoElNegocio(jdbc, negocioA);
        repositorio.save(ArqueoMetodo.de(negocioA, sesionA, new LineaDeArqueo(
                MetodoPagoCaja.EFECTIVO, true, new BigDecimal("120000"), new BigDecimal("115000"), 3)));
        em.flush();
        em.clear();

        BigDecimal diferencia = jdbc.queryForObject(
                "select diferencia from caja.arqueo_metodos where sesion_id = ?", BigDecimal.class, sesionA);

        assertThat(diferencia).isEqualByComparingTo("-5000");
        assertThat(repositorio.findBySesionIdOrderByMetodoPago(sesionA).get(0).getDiferencia())
                .isEqualByComparingTo("-5000");
    }

    @Test
    @DisplayName("HU-140 C2 · Dado un método no cuadrable con contado distinto del esperado, la base lo rechaza aunque la app falle")
    void laBaseExigeQueUnNoCuadrableNoTengaDiferencia() {
        comoElNegocio(jdbc, negocioA);

        assertThatThrownBy(() -> jdbc.update("""
                insert into caja.arqueo_metodos
                  (id, negocio_id, sesion_id, metodo_pago, cuadrable, esperado, contado, movimientos)
                values (?, ?, ?, 'TARJETA_DEBITO', false, 80000, 70000, 2)
                """, UUID.randomUUID(), negocioA, sesionA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-140 C12 · Dados dos negocios, cuando el negocio B consulta, no ve el arqueo del negocio A")
    void cadaNegocioVeSoloSuArqueo() {
        comoElNegocio(jdbc, negocioA);
        repositorio.save(ArqueoMetodo.de(negocioA, sesionA, new LineaDeArqueo(
                MetodoPagoCaja.EFECTIVO, true, new BigDecimal("1000"), new BigDecimal("1000"), 1)));
        em.flush();
        comoElNegocio(jdbc, negocioB);
        repositorio.save(ArqueoMetodo.de(negocioB, sesionB, new LineaDeArqueo(
                MetodoPagoCaja.EFECTIVO, true, new BigDecimal("2000"), new BigDecimal("2000"), 1)));
        em.flush();
        em.clear();

        assertThat(repositorio.findBySesionIdOrderByMetodoPago(sesionA)).isEmpty();
        assertThat(repositorio.findBySesionIdOrderByMetodoPago(sesionB)).hasSize(1);

        comoElNegocio(jdbc, negocioA);
        assertThat(repositorio.findBySesionIdOrderByMetodoPago(sesionA)).hasSize(1);
    }

    private UUID sesionDe(UUID negocio) {
        comoElNegocio(jdbc, negocio);
        UUID caja = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        jdbc.update("insert into caja.cajas (id, negocio_id, codigo, nombre) values (?, ?, ?, 'Caja')",
                caja, negocio, "C-" + caja.toString().substring(0, 8));
        jdbc.update("""
                insert into caja.sesiones_caja (id, negocio_id, caja_id, numero, usuario_apertura_id)
                values (?, ?, ?, ?, ?)
                """, sesion, negocio, caja, "CAJA-" + sesion.toString().substring(0, 8), UUID.randomUUID());
        return sesion;
    }
}
