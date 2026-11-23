package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.application.LibroMayorDeInventario;
import com.regenta.inventario.application.SolicitudDeMovimiento;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-181 · I30: el libro mayor avisa {@code stock_bajo_minimo} cuando un movimiento deja el producto
 * en su mínimo o por debajo en una bodega, y {@code stock_normalizado} cuando vuelve a pasarlo.
 */
@DataJpaTest
class StockBajoMinimoEnLaBaseTest extends PostgresDePrueba {

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

    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final UUID negocio = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private int consecutivo;

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private LibroMayorDeInventario libro() {
        return new LibroMayorDeInventario(movimientos, existencias, productos, lotes, existenciasLote,
                autorizaciones, eventos);
    }

    /** Mínimo 10, objetivo 50: el producto de los criterios. */
    private Producto producto(BigDecimal minimo) {
        comoElNegocio(jdbc, negocio);
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("INVENTARIO"), Set.of(), Set.of()));
        UUID categoria = UUID.randomUUID();
        UUID unidad = UUID.randomUUID();
        jdbc.update("insert into inventario.categorias (id, negocio_id, nombre) values (?, ?, 'Droguería')", categoria, negocio);
        jdbc.update("insert into inventario.unidades_medida (id, negocio_id, codigo, nombre) values (?, ?, 'UND', 'Unidad')", unidad, negocio);
        jdbc.update("insert into inventario.bodegas (id, negocio_id, codigo, nombre) values (?, ?, 'PPAL', 'Principal')", bodega, negocio);
        Producto p = Producto.nuevo(negocio, "ACE-500", "Acetaminofén 500 mg", categoria, unidad, TarifaIva.IVA_19,
                UUID.randomUUID());
        p.configuracionDeStock(true, minimo, new BigDecimal("50"), false, false, false, false);
        return productos.saveAndFlush(p);
    }

    private void mover(LibroMayorDeInventario libro, Producto p, TipoMovimiento tipo, String cantidad) {
        libro.registrar(new SolicitudDeMovimiento(p.getId(), bodega, tipo, new BigDecimal(cantidad),
                OrigenMovimiento.AJUSTE, UUID.randomUUID(), "prueba", "prueba:" + (consecutivo++)));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> publicados(String tipo) {
        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(eventos, atLeast(0)).registrar(eq(negocio), anyString(), any(), eq(tipo), datos.capture());
        return datos.getAllValues().stream().map(d -> (Map<String, Object>) d).toList();
    }

    @Test
    @DisplayName("C1 · Una venta que deja 8 con mínimo 10 publica stock_bajo_minimo con existencia, mínimo y objetivo")
    void publicaAlBajar() {
        Producto p = producto(BigDecimal.TEN);
        LibroMayorDeInventario libro = libro();
        mover(libro, p, TipoMovimiento.ENTRADA_AJUSTE, "20");
        assertThat(publicados("stock_bajo_minimo")).isEmpty();

        mover(libro, p, TipoMovimiento.SALIDA_VENTA, "12");

        List<Map<String, Object>> bajos = publicados("stock_bajo_minimo");
        assertThat(bajos).hasSize(1);
        Map<String, Object> datos = bajos.get(0);
        assertThat(datos.get("producto_id")).isEqualTo(p.getId().toString());
        assertThat(datos.get("bodega_id")).isEqualTo(bodega.toString());
        assertThat(datos.get("producto_nombre")).isEqualTo("Acetaminofén 500 mg");
        assertThat((BigDecimal) datos.get("existencia")).isEqualByComparingTo("8");
        assertThat((BigDecimal) datos.get("minimo")).isEqualByComparingTo("10");
        assertThat((BigDecimal) datos.get("stock_maximo")).isEqualByComparingTo("50");
    }

    @Test
    @DisplayName("C3 · Cada movimiento que lo deja bajo el mínimo vuelve a avisar, con la existencia del momento")
    void avisaDeNuevoConLaExistenciaNueva() {
        Producto p = producto(BigDecimal.TEN);
        LibroMayorDeInventario libro = libro();
        mover(libro, p, TipoMovimiento.ENTRADA_AJUSTE, "20");
        mover(libro, p, TipoMovimiento.SALIDA_VENTA, "12");
        mover(libro, p, TipoMovimiento.SALIDA_VENTA, "3");

        assertThat(publicados("stock_bajo_minimo")).extracting(d -> ((BigDecimal) d.get("existencia")).intValue())
                .containsExactly(8, 5);
    }

    @Test
    @DisplayName("Al volver por encima del mínimo se publica stock_normalizado")
    void normaliza() {
        Producto p = producto(BigDecimal.TEN);
        LibroMayorDeInventario libro = libro();
        mover(libro, p, TipoMovimiento.ENTRADA_AJUSTE, "8");
        mover(libro, p, TipoMovimiento.ENTRADA_AJUSTE, "30");

        assertThat(publicados("stock_normalizado")).hasSize(1);
    }

    @Test
    @DisplayName("Sin mínimo configurado no se avisa nada")
    void sinMinimo() {
        Producto p = producto(BigDecimal.ZERO);
        LibroMayorDeInventario libro = libro();
        mover(libro, p, TipoMovimiento.ENTRADA_AJUSTE, "2");
        mover(libro, p, TipoMovimiento.SALIDA_VENTA, "1");

        verify(eventos, never()).registrar(any(), anyString(), any(), eq("stock_bajo_minimo"), any());
    }
}
