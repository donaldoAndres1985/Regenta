package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-176 · Anular o cerrar una orden contra PostgreSQL (compras V16 y usuarios V16). */
@DataJpaTest
class OrdenAnuladaEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private OrdenDeCompraRepositorio ordenes;
    @Autowired
    private JdbcTemplate jdbc;

    private OrdenDeCompra orden(UUID negocio) {
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Cementos SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        return OrdenDeCompra.crear(negocio, "OC-1", p.getId(), UUID.randomUUID(), null, null, null, BigDecimal.ZERO,
                null);
    }

    @Test
    @DisplayName("C1 · La anulación guarda motivo, usuario y fecha")
    void anulada() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        OrdenDeCompra o = orden(negocio);
        UUID usuario = UUID.randomUUID();
        o.anular(usuario, "El proveedor cerró", false);
        ordenes.saveAndFlush(o);

        Map<String, Object> fila = jdbc.queryForMap("select estado, motivo_anulacion, anulada_por, anulada_en "
                + "from compras.ordenes_compra where id = ?", o.getId());
        assertThat(fila.get("estado")).isEqualTo("ANULADA");
        assertThat(fila.get("motivo_anulacion")).isEqualTo("El proveedor cerró");
        assertThat(fila.get("anulada_por")).isEqualTo(usuario);
        assertThat(fila.get("anulada_en")).isNotNull();
    }

    @Test
    @DisplayName("V16 · Una orden ANULADA sin motivo no entra a la tabla")
    void anuladaSinMotivo() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        OrdenDeCompra o = ordenes.saveAndFlush(orden(negocio));

        assertThatThrownBy(() -> jdbc.update("update compras.ordenes_compra set estado = 'ANULADA' where id = ?",
                o.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V16 · Una orden CERRADA con motivo en blanco no entra a la tabla")
    void cerradaSinMotivo() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        OrdenDeCompra o = ordenes.saveAndFlush(orden(negocio));

        assertThatThrownBy(() -> jdbc.update("update compras.ordenes_compra set estado = 'CERRADA', "
                + "motivo_cierre = '  ' where id = ?", o.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("usuarios V16 · existe COMPRAS_COMPRA_ANULAR y lo traen Administrador y Gerente")
    void permiso() {
        assertThat(jdbc.queryForObject("select count(*) from core_identidad.plantilla_rol_permisos "
                + "where permiso_codigo = 'COMPRAS_COMPRA_ANULAR'", Integer.class)).isEqualTo(2);
    }
}
