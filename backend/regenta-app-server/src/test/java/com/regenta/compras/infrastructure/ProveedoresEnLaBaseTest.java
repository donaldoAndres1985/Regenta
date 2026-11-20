package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-166 · proveedores contra PostgreSQL, con dos negocios (I3, I5, I34). */
@DataJpaTest
class ProveedoresEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private ProveedorProductoRepositorio catalogo;
    @Autowired
    private CuentaPorPagarRepositorio cuentas;
    @Autowired
    private JdbcTemplate jdbc;

    private Proveedor proveedor(UUID negocio, String nombre, String nit) {
        return proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, nit, nombre,
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
    }

    private CuentaPorPagar cuenta(UUID negocio, Proveedor p, String factura, String monto) {
        return cuentas.saveAndFlush(CuentaPorPagar.abrirPorRecepcion(negocio, p.getId(), null, factura,
                new BigDecimal(monto), LocalDate.of(2026, 11, 1), 30));
    }

    @Test
    @DisplayName("HU-166 C4 · La base no deja dos proveedores preferidos para el mismo producto")
    void unPreferidoEnLaBase() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor a = proveedor(negocio, "Proveedor A", "800000001");
        Proveedor b = proveedor(negocio, "Proveedor B", "800000002");
        UUID producto = UUID.randomUUID();
        catalogo.saveAndFlush(ProveedorProducto.de(negocio, a.getId(), producto, null, BigDecimal.ONE, null, null, true));

        assertThatThrownBy(() -> catalogo.saveAndFlush(
                ProveedorProducto.de(negocio, b.getId(), producto, null, BigDecimal.ONE, null, null, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("HU-166 C4 · Desmarcar a los demás deja a B como único preferido")
    void quitarPreferidoDeLosDemas() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor a = proveedor(negocio, "Proveedor A", "800000001");
        Proveedor b = proveedor(negocio, "Proveedor B", "800000002");
        UUID producto = UUID.randomUUID();
        catalogo.saveAndFlush(ProveedorProducto.de(negocio, a.getId(), producto, null, BigDecimal.ONE, null, null, true));

        int desmarcados = catalogo.quitarPreferidoDeLosDemas(negocio, producto, b.getId());
        catalogo.saveAndFlush(ProveedorProducto.de(negocio, b.getId(), producto, null, BigDecimal.ONE, null, null, true));

        assertThat(desmarcados).isEqualTo(1);
        assertThat(catalogo.findByProveedorIdAndProductoId(a.getId(), producto).orElseThrow().isPreferido()).isFalse();
        assertThat(catalogo.findByProveedorIdAndProductoId(b.getId(), producto).orElseThrow().isPreferido()).isTrue();
    }

    @Test
    @DisplayName("HU-166 C5 · La deuda suma las cuentas pendientes y parciales; las pagadas no cuentan")
    void deudaPorProveedor() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedor(negocio, "Distribuidora Andina", "900123456");
        cuenta(negocio, p, "FV-1", "300000");
        CuentaPorPagar parcial = cuenta(negocio, p, "FV-2", "700000");
        parcial.registrarPago(new BigDecimal("200000"));
        cuentas.saveAndFlush(parcial);
        CuentaPorPagar pagada = cuenta(negocio, p, "FV-3", "90000");
        pagada.registrarPago(new BigDecimal("90000"));
        cuentas.saveAndFlush(pagada);

        List<Object[]> filas = cuentas.saldosAbiertosPorProveedor(negocio);

        assertThat(filas).hasSize(1);
        assertThat(filas.get(0)[0]).isEqualTo(p.getId());
        assertThat((BigDecimal) filas.get(0)[1]).isEqualByComparingTo("800000");
    }

    @Test
    @DisplayName("HU-166 C6 · El negocio B no ve los proveedores ni la deuda del negocio A")
    void dosNegocios() {
        UUID negocioA = UUID.randomUUID();
        UUID negocioB = UUID.randomUUID();
        comoElNegocio(jdbc, negocioA);
        Proveedor deA = proveedor(negocioA, "Proveedor de A", "900000001");
        cuenta(negocioA, deA, "FV-1", "500000");

        comoElNegocio(jdbc, negocioB);
        proveedor(negocioB, "Proveedor de B", "900000002");

        assertThat(proveedores.findByNegocioIdOrderByRazonSocialAsc(negocioB))
                .extracting(Proveedor::getRazonSocial).containsExactly("Proveedor de B");
        assertThat(cuentas.saldosAbiertosPorProveedor(negocioB)).isEmpty();
    }
}
