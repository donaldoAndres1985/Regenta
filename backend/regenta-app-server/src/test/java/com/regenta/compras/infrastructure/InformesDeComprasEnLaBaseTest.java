package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-183 · Las consultas de los informes de compras contra PostgreSQL, y el permiso nuevo (usuarios V18). */
@DataJpaTest
class InformesDeComprasEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private RecepcionRepositorio recepciones;
    @Autowired
    private RecepcionLineaRepositorio lineas;
    @Autowired
    private DevolucionAProveedorRepositorio devoluciones;
    @Autowired
    private LineaDeDevolucionAProveedorRepositorio lineasDevueltas;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();

    @Test
    @DisplayName("Recepciones por estado y periodo, sus líneas, las entradas de un producto y las devoluciones")
    void consultas() {
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        UUID producto = UUID.randomUUID();
        Recepcion r = Recepcion.crear(negocio, "REC-1", null, p.getId(), UUID.randomUUID(), "FV-1", null, null);
        RecepcionLinea l = RecepcionLinea.nueva(r.getId(), negocio, new RecepcionLinea.Datos(null, producto,
                "Acetaminofén", null, BigDecimal.TEN, new BigDecimal("10000"), BigDecimal.ZERO, "IVA_19",
                new BigDecimal("19"), false, false, null, null, null), LocalDate.now());
        r.totalizar(List.of(l), true);
        r.confirmar();
        recepciones.saveAndFlush(r);
        lineas.saveAndFlush(l);
        Recepcion borrador = recepciones.saveAndFlush(
                Recepcion.crear(negocio, "REC-2", null, p.getId(), UUID.randomUUID(), null, null, null));
        DevolucionAProveedor d = devoluciones.saveAndFlush(DevolucionAProveedor.registrar(negocio, "DEV-1", r,
                "Llegó roto", new BigDecimal("11900"), null));
        lineasDevueltas.saveAndFlush(LineaDeDevolucionAProveedor.de(negocio, d.getId(), l, BigDecimal.ONE,
                new BigDecimal("10000"), new BigDecimal("1900")));

        OffsetDateTime desde = OffsetDateTime.now().minusHours(1);
        OffsetDateTime hasta = OffsetDateTime.now().plusHours(1);
        assertThat(recepciones.findByNegocioIdAndEstadoAndFechaGreaterThanEqualAndFechaLessThan(negocio,
                EstadoRecepcion.CONFIRMADA, desde, hasta)).extracting(Recepcion::getId).containsExactly(r.getId());
        assertThat(recepciones.findByNegocioIdAndEstadoAndFechaGreaterThanEqualAndFechaLessThan(negocio,
                EstadoRecepcion.CONFIRMADA, hasta, hasta.plusDays(1))).isEmpty();
        assertThat(lineas.findByRecepcionIdIn(List.of(r.getId(), borrador.getId()))).hasSize(1);
        assertThat(lineas.findByNegocioIdAndProductoId(negocio, producto)).extracting(RecepcionLinea::getId)
                .containsExactly(l.getId());
        assertThat(devoluciones.findByNegocioIdAndFechaGreaterThanEqualAndFechaLessThan(negocio, desde, hasta))
                .hasSize(1);
        assertThat(lineasDevueltas.findByDevolucionIdIn(List.of(d.getId()))).hasSize(1);
    }

    @Test
    @DisplayName("C5 · usuarios V18: existe COMPRAS_INFORME_VER y lo traen Administrador y Gerente")
    void permisoNuevo() {
        Integer permiso = jdbc.queryForObject(
                "select count(*) from core_identidad.permisos where codigo = 'COMPRAS_INFORME_VER'", Integer.class);
        Integer plantillas = jdbc.queryForObject("select count(*) from core_identidad.plantilla_rol_permisos "
                + "where permiso_codigo = 'COMPRAS_INFORME_VER'", Integer.class);
        assertThat(permiso).isEqualTo(1);
        assertThat(plantillas).isEqualTo(2);
    }
}
