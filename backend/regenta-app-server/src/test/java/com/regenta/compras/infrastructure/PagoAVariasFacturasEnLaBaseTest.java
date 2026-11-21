package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.AplicacionDePago;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-171 · Pagos a varias facturas contra PostgreSQL (migración V13). */
@DataJpaTest
class PagoAVariasFacturasEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private CuentaPorPagarRepositorio cuentas;
    @Autowired
    private PagoProveedorRepositorio pagos;
    @Autowired
    private AplicacionDePagoRepositorio aplicaciones;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-171 · Un pago con dos aplicaciones; la misma cuenta no se aplica dos veces en un pago")
    void dosAplicaciones() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        CuentaPorPagar a = cuentas.saveAndFlush(CuentaPorPagar.abrirPorRecepcion(negocio, p.getId(), null, "FV-1",
                new BigDecimal("300000"), LocalDate.of(2026, 10, 1), 30));
        CuentaPorPagar b = cuentas.saveAndFlush(CuentaPorPagar.abrirPorRecepcion(negocio, p.getId(), null, "FV-2",
                new BigDecimal("500000"), LocalDate.of(2026, 11, 1), 30));
        PagoProveedor pago = pagos.saveAndFlush(PagoProveedor.registrar(negocio, p.getId(), null,
                new BigDecimal("400000"), MetodoDePago.TRANSFERENCIA, null, null, null));

        aplicaciones.saveAndFlush(AplicacionDePago.de(negocio, pago.getId(), a.getId(), new BigDecimal("300000")));
        aplicaciones.saveAndFlush(AplicacionDePago.de(negocio, pago.getId(), b.getId(), new BigDecimal("100000")));

        assertThat(aplicaciones.findByPagoId(pago.getId())).hasSize(2);
        assertThat(jdbc.queryForObject("select cuenta_id from compras.pagos_proveedor where id = ?", UUID.class,
                pago.getId())).isNull();
        assertThatThrownBy(() -> aplicaciones.saveAndFlush(
                AplicacionDePago.de(negocio, pago.getId(), a.getId(), new BigDecimal("1"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-171 · Después del relleno, las tablas siguen con RLS forzado y la nueva también")
    void rlsForzado() {
        for (String tabla : new String[] { "pagos_proveedor", "cuentas_por_pagar", "pago_aplicaciones" }) {
            Boolean forzado = jdbc.queryForObject("select relforcerowsecurity from pg_class c "
                    + "join pg_namespace n on n.oid = c.relnamespace where n.nspname = 'compras' and c.relname = ?",
                    Boolean.class, tabla);
            assertThat(forzado).as(tabla).isTrue();
        }
    }
}
