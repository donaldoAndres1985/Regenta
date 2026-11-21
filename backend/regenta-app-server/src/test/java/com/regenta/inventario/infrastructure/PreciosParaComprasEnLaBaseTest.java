package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
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
import com.regenta.inventario.api.CostoYPrecio;
import com.regenta.inventario.application.EntradaDeCompras;
import com.regenta.inventario.application.EntradaDeRecepcion;
import com.regenta.inventario.application.LibroMayorDeInventario;
import com.regenta.inventario.application.PreciosParaCompras;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-179 · El costo, el precio y el precio nuevo de un producto, contra PostgreSQL. */
@DataJpaTest
class PreciosParaComprasEnLaBaseTest extends PostgresDePrueba {

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

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("C1 / C2 · Costo promedio $ 9.000 en la bodega, precio $ 14.280 con base $ 12.000; el precio nuevo queda guardado")
    void costoPrecioYPrecioNuevo() {
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
        p.cambiarPrecioDeVenta(new BigDecimal("14280"));
        p = productos.saveAndFlush(p);
        LibroMayorDeInventario libro = new LibroMayorDeInventario(movimientos, existencias, productos, lotes,
                existenciasLote, autorizaciones, mock(RegistroDeEventos.class));
        new EntradaDeCompras(libro, existencias, lotes).registrar(new EntradaDeRecepcion(UUID.randomUUID(), "REC-0",
                bodega, List.of(new EntradaDeRecepcion.Linea(p.getId(), new BigDecimal("20"), new BigDecimal("9000"),
                        null, null, null))));
        PreciosParaCompras precios = new PreciosParaCompras(productos, existencias);

        CostoYPrecio visto = precios.costosYPrecios(negocio, bodega, List.of(p.getId())).get(p.getId());
        assertThat(visto.costoPromedio()).isEqualByComparingTo("9000");
        assertThat(visto.precio()).isEqualByComparingTo("14280");
        assertThat(visto.base()).isEqualByComparingTo("12000");

        precios.cambiarPrecioDeVenta(negocio, p.getId(), new BigDecimal("15470"));
        productos.flush();
        assertThat(jdbc.queryForObject("select precio_venta from inventario.productos where id = ?", BigDecimal.class,
                p.getId())).isEqualByComparingTo("15470");
        assertThat(precios.costosYPrecios(negocio, bodega, List.of(p.getId())).get(p.getId()).base())
                .isEqualByComparingTo("13000");
    }
}
