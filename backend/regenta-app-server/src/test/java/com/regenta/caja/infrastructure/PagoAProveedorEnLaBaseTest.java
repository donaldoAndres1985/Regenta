package com.regenta.caja.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;

/** HU-145 · Las migraciones del pago a proveedor desde la caja, contra PostgreSQL. */
@DataJpaTest
class PagoAProveedorEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-145 · caja V10: la base acepta una salida PAGO_PROVEEDOR en efectivo")
    void laCajaAceptaElTipo() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        UUID caja = UUID.randomUUID();
        UUID sesion = UUID.randomUUID();
        jdbc.update("insert into caja.cajas (id, negocio_id, codigo, nombre) values (?, ?, 'C-1', 'Caja')", caja, negocio);
        jdbc.update("insert into caja.sesiones_caja (id, negocio_id, caja_id, numero, usuario_apertura_id) "
                + "values (?, ?, ?, 'CAJA-1', ?)", sesion, negocio, caja, UUID.randomUUID());

        jdbc.update("""
                insert into caja.movimientos_caja
                  (id, negocio_id, sesion_id, tipo, signo, metodo_pago, monto, concepto, usuario_id,
                   idempotency_key, origen_tipo, documento_ref)
                values (?, ?, ?, 'PAGO_PROVEEDOR', -1, 'EFECTIVO', 100000, 'Pago a proveedor Andina', ?,
                        'PAGO_PROVEEDOR:1', 'PAGO_PROVEEDOR', 'FV-100')
                """, UUID.randomUUID(), negocio, sesion, UUID.randomUUID());

        Integer salidas = jdbc.queryForObject("select count(*) from caja.movimientos_caja "
                + "where sesion_id = ? and tipo = 'PAGO_PROVEEDOR' and signo = -1", Integer.class, sesion);
        assertThat(salidas).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-145 · compras V10: el pago guarda la sesión de caja de la que salió")
    void elPagoGuardaLaSesion() {
        String tipo = jdbc.queryForObject("select data_type from information_schema.columns "
                + "where table_schema = 'compras' and table_name = 'pagos_proveedor' and column_name = 'sesion_caja_id'",
                String.class);
        assertThat(tipo).isEqualTo("uuid");
    }
}
