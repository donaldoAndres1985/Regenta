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
import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.NotaCreditoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-178 · Devoluciones y notas crédito contra PostgreSQL (compras V18 y usuarios V17). */
@DataJpaTest
class DevolucionEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private RecepcionRepositorio recepciones;
    @Autowired
    private RecepcionLineaRepositorio recepcionLineas;
    @Autowired
    private CuentaPorPagarRepositorio cuentas;
    @Autowired
    private DevolucionAProveedorRepositorio devoluciones;
    @Autowired
    private LineaDeDevolucionAProveedorRepositorio lineas;
    @Autowired
    private NotaCreditoRepositorio notas;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("C1 / C3 · Se guardan la devolución, sus líneas y la nota; el excedente queda como saldo a favor")
    void seGuarda() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        Recepcion r = Recepcion.crear(negocio, "REC-1", null, p.getId(), UUID.randomUUID(), "FV-100", null, null);
        RecepcionLinea a = RecepcionLinea.nueva(r.getId(), negocio, null, UUID.randomUUID(), BigDecimal.TEN,
                new BigDecimal("10000"), true, false, LocalDate.now(), "L-1", null, null);
        r.totalizar(List.of(a));
        r.confirmar();
        r.registrarDevolucion();
        recepciones.saveAndFlush(r);
        recepcionLineas.saveAndFlush(a);
        CuentaPorPagar cuenta = CuentaPorPagar.abrirPorRecepcion(negocio, p.getId(), r.getId(), "FV-100",
                new BigDecimal("10000"), LocalDate.now(), 30);
        cuentas.saveAndFlush(cuenta);

        DevolucionAProveedor d = devoluciones.saveAndFlush(DevolucionAProveedor.registrar(negocio, "DEV-1", r,
                "Averiado", new BigDecimal("23800"), null));
        lineas.saveAndFlush(LineaDeDevolucionAProveedor.de(negocio, d.getId(), a, new BigDecimal("2"), new BigDecimal("20000"),
                new BigDecimal("3800")));
        NotaCreditoProveedor nota = notas.saveAndFlush(NotaCreditoProveedor.emitir(negocio, d, cuenta,
                new BigDecimal("23800")));
        cuentas.saveAndFlush(cuenta);

        assertThat(nota.getAplicadoACuenta()).isEqualByComparingTo("10000");
        assertThat(notas.saldoAFavorDe(negocio, p.getId())).isEqualByComparingTo("13800");
        assertThat(notas.saldosAFavorPorProveedor(negocio)).hasSize(1);
        assertThat(lineas.findByRecepcionLineaIdIn(List.of(a.getId()))).extracting(LineaDeDevolucionAProveedor::getCodigoLote)
                .containsExactly("L-1");
        assertThat(jdbc.queryForObject("select con_devoluciones from compras.recepciones where id = ?", Boolean.class,
                r.getId())).isTrue();
        assertThat(jdbc.queryForObject("select estado from compras.cuentas_por_pagar where id = ?", String.class,
                cuenta.getId())).isEqualTo("PAGADA");
        for (String tabla : List.of("devoluciones_proveedor", "devolucion_lineas", "notas_credito_proveedor")) {
            assertThat(jdbc.queryForObject("select relforcerowsecurity from pg_class c join pg_namespace n "
                    + "on n.oid = c.relnamespace where n.nspname = 'compras' and c.relname = ?", Boolean.class, tabla))
                    .as(tabla).isTrue();
        }
    }

    @Test
    @DisplayName("V18 · Una nota cuyo reparto no suma su monto no entra a la tabla")
    void repartoDeLaNota() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123457",
                "Otro SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        Recepcion r = recepciones.saveAndFlush(Recepcion.crear(negocio, "REC-2", null, p.getId(), UUID.randomUUID(),
                null, null, null));
        DevolucionAProveedor d = devoluciones.saveAndFlush(DevolucionAProveedor.registrar(negocio, "DEV-2", r,
                "Vencido", new BigDecimal("100"), null));

        assertThatThrownBy(() -> jdbc.update("insert into compras.notas_credito_proveedor (id, negocio_id, "
                + "proveedor_id, devolucion_id, monto, aplicado_a_cuenta, excedente, saldo_a_favor) "
                + "values (?, ?, ?, ?, 100, 30, 30, 30)", UUID.randomUUID(), negocio, p.getId(), d.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("usuarios V17 · existe COMPRAS_DEVOLUCION_REGISTRAR y lo traen Administrador y Gerente")
    void permiso() {
        assertThat(jdbc.queryForObject("select count(*) from core_identidad.plantilla_rol_permisos "
                + "where permiso_codigo = 'COMPRAS_DEVOLUCION_REGISTRAR'", Integer.class)).isEqualTo(2);
    }
}
