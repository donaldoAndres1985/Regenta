package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.api.SalidaPrevista;
import com.regenta.inventario.application.EntradaDeCompras;
import com.regenta.inventario.application.EntradaDeRecepcion;
import com.regenta.inventario.application.LibroMayorDeInventario;
import com.regenta.inventario.application.SalidaPorAnulacionDeCompra;
import com.regenta.inventario.application.SolicitudDeMovimiento;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-177 · Anular una recepción saca lo que entró, contra PostgreSQL (inventario V13). */
@DataJpaTest
class SalidaPorAnulacionEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private MovimientoInventarioRepositorio movimientos;
    @Autowired
    private ExistenciaRepositorio existencias;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private LoteRepositorio lotes;
    @Autowired
    private ExistenciaLoteRepositorio existenciasLote;
    @Autowired
    private AutorizacionLoteVencidoRepositorio autorizaciones;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private LibroMayorDeInventario libro;

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private Producto producto(boolean conLotes) {
        comoElNegocio(jdbc, negocio);
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("INVENTARIO"), Set.of(), Set.of()));
        UUID categoria = UUID.randomUUID();
        UUID unidad = UUID.randomUUID();
        jdbc.update("insert into inventario.categorias (id, negocio_id, nombre) values (?, ?, 'Droguería')", categoria, negocio);
        jdbc.update("insert into inventario.unidades_medida (id, negocio_id, codigo, nombre) values (?, ?, 'UND', 'Unidad')", unidad, negocio);
        jdbc.update("insert into inventario.bodegas (id, negocio_id, codigo, nombre) values (?, ?, 'PPAL', 'Principal')", bodega, negocio);
        Producto p = Producto.nuevo(negocio, "ACE-500", "Acetaminofén 500 mg", categoria, unidad, TarifaIva.IVA_19,
                UUID.randomUUID());
        p.configuracionDeStock(null, null, null, conLotes, false, conLotes, false);
        libro = new LibroMayorDeInventario(movimientos, existencias, productos, lotes, existenciasLote, autorizaciones,
                mock(RegistroDeEventos.class));
        return productos.saveAndFlush(p);
    }

    private SalidaPorAnulacionDeCompra salida() {
        return new SalidaPorAnulacionDeCompra(libro, existencias, existenciasLote, lotes, productos);
    }

    private EntradaDeRecepcion recepcion(Producto p, String lote) {
        return new EntradaDeRecepcion(recepcion, "REC-1", bodega, List.of(new EntradaDeRecepcion.Linea(p.getId(),
                BigDecimal.TEN, new BigDecimal("10500"), lote, lote == null ? null : LocalDate.now().plusMonths(8),
                null)));
    }

    private void vender(Producto p, String cantidad, String lote) {
        libro.registrar(new SolicitudDeMovimiento(p.getId(), bodega, TipoMovimiento.SALIDA_VENTA,
                new BigDecimal(cantidad), OrigenMovimiento.VENTA, UUID.randomUUID(), "Venta",
                "venta:" + UUID.randomUUID(), lote, null, null));
    }

    @Test
    @DisplayName("C1 · Sin ventas de por medio, la anulación saca las 10 und con un movimiento propio, una sola vez")
    void sacaLoQueEntro() {
        Producto p = producto(false);
        new EntradaDeCompras(libro, existencias, lotes).registrar(recepcion(p, null));

        assertThat(salida().faltantes(negocio, bodega, List.of(new SalidaPrevista(p.getId(), BigDecimal.TEN, null))))
                .isEmpty();
        salida().sacar(recepcion(p, null));
        salida().sacar(recepcion(p, null));

        assertThat(existencias.findByProductoIdAndBodegaId(p.getId(), bodega).orElseThrow().getCantidad())
                .isEqualByComparingTo("0");
        assertThat(jdbc.queryForList("select tipo from inventario.movimientos_inventario where origen_id = ? "
                + "order by ocurrido_en", String.class, recepcion))
                .containsExactly("ENTRADA_COMPRA", "SALIDA_ANULACION_COMPRA");
    }

    @Test
    @DisplayName("C3 · De 10 und se vendieron 4: quedan 6 y no alcanza para anular")
    void yaSeVendioParte() {
        Producto p = producto(false);
        new EntradaDeCompras(libro, existencias, lotes).registrar(recepcion(p, null));
        vender(p, "4", null);

        assertThat(salida().faltantes(negocio, bodega, List.of(new SalidaPrevista(p.getId(), BigDecimal.TEN, null))))
                .containsExactly("De «Acetaminofén 500 mg» quedan 6 y se tendrían que sacar 10");
    }

    @Test
    @DisplayName("C4 · Con lote, sale del mismo lote que entró; si de ese lote ya se vendió, no alcanza")
    void delMismoLote() {
        Producto p = producto(true);
        new EntradaDeCompras(libro, existencias, lotes).registrar(recepcion(p, "L-1"));
        var lote = lotes.findByNegocioIdAndProductoIdAndCodigoLote(negocio, p.getId(), "L-1").orElseThrow();

        salida().sacar(recepcion(p, "L-1"));

        assertThat(existenciasLote.findByLoteIdAndBodegaId(lote.getId(), bodega).orElseThrow().getCantidad())
                .isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("select lote_id from inventario.movimientos_inventario where origen_id = ? "
                + "and tipo = 'SALIDA_ANULACION_COMPRA'", UUID.class, recepcion)).isEqualTo(lote.getId());
    }

    @Test
    @DisplayName("C4 · Si del lote recibido ya se vendió parte, aunque haya stock de otro lote, no alcanza")
    void otroLoteNoSirve() {
        Producto p = producto(true);
        new EntradaDeCompras(libro, existencias, lotes).registrar(recepcion(p, "L-1"));
        new EntradaDeCompras(libro, existencias, lotes).registrar(new EntradaDeRecepcion(UUID.randomUUID(), "REC-2",
                bodega, List.of(new EntradaDeRecepcion.Linea(p.getId(), new BigDecimal("50"), new BigDecimal("10500"),
                        "L-2", LocalDate.now().plusMonths(9), null))));
        vender(p, "3", "L-1");

        assertThat(salida().faltantes(negocio, bodega, List.of(new SalidaPrevista(p.getId(), BigDecimal.TEN, "L-1"))))
                .containsExactly("Del lote L-1 de «Acetaminofén 500 mg» quedan 7 y se tendrían que sacar 10");
    }
}
