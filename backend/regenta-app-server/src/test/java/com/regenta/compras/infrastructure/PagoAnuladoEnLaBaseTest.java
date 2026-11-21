package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-172 · Anular un pago, contra PostgreSQL (compras V14 y usuarios V14). */
@DataJpaTest
class PagoAnuladoEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private PagoProveedorRepositorio pagos;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-172 C2 · El pago anulado se guarda con motivo, usuario y fecha; sin motivo la base lo rechaza")
    void anuladoConMotivo() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        PagoProveedor pago = pagos.saveAndFlush(PagoProveedor.registrar(negocio, p.getId(), null,
                new BigDecimal("400000"), MetodoDePago.TRANSFERENCIA, null, null, null));
        UUID usuario = UUID.randomUUID();

        pago.anular(usuario, "Se registró dos veces", null);
        pagos.saveAndFlush(pago);

        var fila = jdbc.queryForMap("select anulado_en, anulado_por, motivo_anulacion from compras.pagos_proveedor "
                + "where id = ?", pago.getId());
        assertThat(fila.get("anulado_en")).isNotNull();
        assertThat(fila.get("anulado_por")).isEqualTo(usuario);
        assertThat(fila.get("motivo_anulacion")).isEqualTo("Se registró dos veces");
        assertThatThrownBy(() -> jdbc.update("update compras.pagos_proveedor set motivo_anulacion = null where id = ?",
                pago.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-172 C5 · usuarios V14: existe COMPRAS_PAGO_ANULAR y lo traen Administrador y Gerente")
    void permisoNuevo() {
        Integer permiso = jdbc.queryForObject(
                "select count(*) from core_identidad.permisos where codigo = 'COMPRAS_PAGO_ANULAR'", Integer.class);
        Integer plantillas = jdbc.queryForObject("select count(*) from core_identidad.plantilla_rol_permisos "
                + "where permiso_codigo = 'COMPRAS_PAGO_ANULAR'", Integer.class);
        assertThat(permiso).isEqualTo(1);
        assertThat(plantillas).isEqualTo(2);
    }
}
