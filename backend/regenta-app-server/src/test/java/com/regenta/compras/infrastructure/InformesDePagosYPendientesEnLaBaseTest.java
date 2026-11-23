package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-184 · Las consultas de pendientes por recibir y de pagos contra PostgreSQL. */
@DataJpaTest
class InformesDePagosYPendientesEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private OrdenDeCompraRepositorio ordenes;
    @Autowired
    private OrdenCompraLineaRepositorio lineas;
    @Autowired
    private PagoProveedorRepositorio pagos;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();

    private Proveedor proveedor() {
        return proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
    }

    @Test
    @DisplayName("C2 · Órdenes por estado y las líneas de varias órdenes")
    void ordenesPorEstado() {
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedor();
        OrdenDeCompra aprobada = OrdenDeCompra.crear(negocio, "OC-1", p.getId(), UUID.randomUUID(), null, null,
                LocalDate.now(), BigDecimal.ZERO, null);
        OrdenCompraLinea l = OrdenCompraLinea.nueva(aprobada.getId(), negocio, 1, UUID.randomUUID(), "Acetaminofén",
                BigDecimal.TEN, new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        aprobada.totalizar(List.of(l));
        aprobada.confirmar(null, true, false);
        ordenes.saveAndFlush(aprobada);
        lineas.saveAndFlush(l);
        ordenes.saveAndFlush(OrdenDeCompra.crear(negocio, "OC-2", p.getId(), UUID.randomUUID(), null, null, null,
                BigDecimal.ZERO, null));

        assertThat(ordenes.findByNegocioIdAndEstadoIn(negocio,
                Set.of(EstadoOrdenCompra.APROBADA, EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.PARCIAL)))
                .extracting(OrdenDeCompra::getNumero).containsExactly("OC-1");
        assertThat(lineas.findByOrdenIdInOrderByLinea(List.of(aprobada.getId()))).hasSize(1);
    }

    @Test
    @DisplayName("C4 · Los pagos del periodo, sin los anulados")
    void pagosSinAnulados() {
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedor();
        pagos.saveAndFlush(PagoProveedor.registrar(negocio, p.getId(), null, new BigDecimal("100000"),
                MetodoDePago.TRANSFERENCIA, null, null, null));
        PagoProveedor anulado = PagoProveedor.registrar(negocio, p.getId(), null, new BigDecimal("999"),
                MetodoDePago.CHEQUE, null, null, null);
        anulado.anular(UUID.randomUUID(), "Se registró por error", null);
        pagos.saveAndFlush(anulado);

        LocalDate hoy = LocalDate.now();
        assertThat(pagos.findByNegocioIdAndAnuladoEnIsNullAndFechaBetween(negocio, hoy.minusDays(1), hoy.plusDays(1)))
                .singleElement().satisfies(x -> assertThat(x.getMonto()).isEqualByComparingTo("100000"));
        assertThat(pagos.findByNegocioIdAndAnuladoEnIsNullAndFechaBetween(negocio, hoy.plusDays(1), hoy.plusDays(2)))
                .isEmpty();
    }
}
