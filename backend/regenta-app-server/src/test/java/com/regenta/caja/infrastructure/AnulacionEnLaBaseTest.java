package com.regenta.caja.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;

/** HU-141 · las garantías de la anulación probadas contra PostgreSQL, con dos negocios. */
@DataJpaTest
class AnulacionEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MovimientoDeCajaRepositorio movimientos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private UUID sesionA;
    private UUID retiroA;

    @BeforeEach
    void unRetiroEnElNegocioA() {
        sesionA = sesionDe(negocioA);
        retiroA = movimiento(negocioA, sesionA, "RETIRO", -1, null, null);
        sesionDe(negocioB);
    }

    @Test
    @DisplayName("HU-141 C5 · Dado un movimiento ya anulado, la base rechaza una segunda anulación aunque la app falle")
    void laBaseNoAdmiteDosAnulaciones() {
        comoElNegocio(jdbc, negocioA);
        movimiento(negocioA, sesionA, "ANULACION", 1, retiroA, "Duplicado");

        assertThatThrownBy(() -> movimiento(negocioA, sesionA, "ANULACION", 1, retiroA, "Otra vez"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-141 C3 · Dada una anulación sin motivo, la base la rechaza")
    void laBaseExigeMotivo() {
        comoElNegocio(jdbc, negocioA);

        assertThatThrownBy(() -> movimiento(negocioA, sesionA, "ANULACION", 1, retiroA, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-141 C1 · Dada una anulación sin el movimiento original, la base la rechaza")
    void laBaseExigeElOriginal() {
        comoElNegocio(jdbc, negocioA);

        assertThatThrownBy(() -> movimiento(negocioA, sesionA, "ANULACION", 1, null, "Sin original"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-141 C9 · Dados dos negocios, el negocio B no encuentra el movimiento del negocio A")
    void elNegocioBNoVeElMovimientoDelA() {
        comoElNegocio(jdbc, negocioB);
        assertThat(movimientos.findByIdAndNegocioId(retiroA, negocioA)).isEmpty();
        assertThat(movimientos.existsByAnulaA(retiroA)).isFalse();

        comoElNegocio(jdbc, negocioA);
        assertThat(movimientos.findByIdAndNegocioId(retiroA, negocioA)).isPresent();
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

    private UUID movimiento(UUID negocio, UUID sesion, String tipo, int signo, UUID anulaA,
            String motivo) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into caja.movimientos_caja
                  (id, negocio_id, sesion_id, tipo, signo, metodo_pago, monto, concepto, usuario_id,
                   idempotency_key, anula_a, motivo_anulacion)
                values (?, ?, ?, ?, ?, 'EFECTIVO', 40000, 'x', ?, ?, ?, ?)
                """, id, negocio, sesion, tipo, signo, UUID.randomUUID(), "k-" + id, anulaA, motivo);
        return id;
    }
}
