package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.ConfiguracionDeCompras;

/** HU-173 · La configuración de compras contra PostgreSQL (compras V15 y usuarios V15). */
@DataJpaTest
class ConfiguracionDeComprasEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ConfiguracionDeComprasRepositorio configuraciones;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-173 C5 · El CHECK fijo de 1,05 ya no está en orden_compra_lineas")
    void sinCheckFijo() {
        Integer checks = jdbc.queryForObject("select count(*) from pg_constraint "
                + "where conname = 'ck_recibida_oc' and conrelid = 'compras.orden_compra_lineas'::regclass",
                Integer.class);
        assertThat(checks).isZero();
    }

    @Test
    @DisplayName("HU-173 · Se guarda y se lee la configuración de un negocio; la tabla tiene RLS forzado")
    void seGuarda() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        ConfiguracionDeCompras c = ConfiguracionDeCompras.porOmision(negocio);
        c.cambiar(true, new BigDecimal("1000000"), BigDecimal.ZERO);
        configuraciones.saveAndFlush(c);

        ConfiguracionDeCompras leida = configuraciones.findById(negocio).orElseThrow();
        assertThat(leida.getMontoMinimoAprobacion()).isEqualByComparingTo("1000000");
        assertThat(leida.getToleranciaRecepcionPct()).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select relforcerowsecurity from pg_class c join pg_namespace n "
                + "on n.oid = c.relnamespace where n.nspname = 'compras' and c.relname = 'configuracion'",
                Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("HU-173 · usuarios V15: existe COMPRAS_CONFIGURAR y lo traen Administrador y Gerente")
    void permiso() {
        assertThat(jdbc.queryForObject("select count(*) from core_identidad.plantilla_rol_permisos "
                + "where permiso_codigo = 'COMPRAS_CONFIGURAR'", Integer.class)).isEqualTo(2);
    }
}
