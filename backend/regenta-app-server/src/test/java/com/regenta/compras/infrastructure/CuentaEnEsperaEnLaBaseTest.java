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
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-170 · Cuentas en espera de factura contra PostgreSQL (migración V12). */
@DataJpaTest
class CuentaEnEsperaEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private CuentaPorPagarRepositorio cuentas;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-170 · Dos remisiones del mismo proveedor conviven sin número; un número repetido sigue rechazado")
    void sinNumeroNoChocan() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));

        cuentas.saveAndFlush(CuentaPorPagar.abrirEnEsperaDeFactura(negocio, p.getId(), null,
                new BigDecimal("119000"), LocalDate.of(2026, 11, 20), 30));
        CuentaPorPagar segunda = cuentas.saveAndFlush(CuentaPorPagar.abrirEnEsperaDeFactura(negocio, p.getId(), null,
                new BigDecimal("50000"), LocalDate.of(2026, 11, 21), 30));

        segunda.registrarFactura("FV-300", LocalDate.of(2026, 11, 25), 30);
        cuentas.saveAndFlush(segunda);
        assertThat(jdbc.queryForObject("select fecha_vencimiento from compras.cuentas_por_pagar where id = ?",
                LocalDate.class, segunda.getId())).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(jdbc.queryForObject("select count(*) from compras.cuentas_por_pagar "
                + "where negocio_id = ? and numero_factura is null", Integer.class, negocio)).isEqualTo(1);

        assertThatThrownBy(() -> cuentas.saveAndFlush(CuentaPorPagar.abrirPorRecepcion(negocio, p.getId(), null,
                "FV-300", new BigDecimal("1000"), LocalDate.of(2026, 11, 26), 30)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
