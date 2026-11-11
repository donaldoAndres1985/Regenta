package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.NivelStock;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoCodigoRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * El stock que ve el POS tiene que ser el mismo que de verdad valida la saga
 * de confirmacion (HU-038): cantidad menos lo ya reservado por otras ventas
 * en curso, no la cantidad bruta de la bodega.
 */
class BusquedaDeProductosTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final ProductoCodigoRepositorio codigos = mock(ProductoCodigoRepositorio.class);
    private final CategoriaRepositorio categorias = mock(CategoriaRepositorio.class);
    private final ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);

    private final BusquedaDeProductos busqueda =
            new BusquedaDeProductos(productos, codigos, categorias, existencias);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_PRODUCTO_VER"), Set.of()));
        when(categorias.findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(any())).thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    void elStockMostradoDescuentaLoYaReservadoPorOtrasVentas() {
        Producto producto = Producto.nuevo(negocioId, "MANT-001", "Manto real", UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        when(productos.findByNegocioIdAndActivoTrueOrderByNombreAsc(negocioId))
                .thenReturn(List.of(producto));

        Existencia existencia = Existencia.enCero(negocioId, producto.getId(), bodegaId);
        existencia.aplicar(1, new BigDecimal("3"));
        existencia.reservar(new BigDecimal("3")); // otra venta en curso ya aparto todo
        when(existencias.findByProductoIdOrderByBodegaId(producto.getId()))
                .thenReturn(List.of(existencia));

        ResultadoDeBusqueda resultado = busqueda.buscar(new FiltroDeBusqueda(null, null, false, null));

        ProductoEncontrado fila = resultado.productos().get(0);
        assertThat(fila.stockTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fila.nivelStock()).isEqualTo(NivelStock.CERO);
    }

    @Test
    void sinReservasElStockEsLaCantidadCompleta() {
        Producto producto = Producto.nuevo(negocioId, "MANT-002", "Manto real 2", UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID());
        when(productos.findByNegocioIdAndActivoTrueOrderByNombreAsc(negocioId))
                .thenReturn(List.of(producto));

        Existencia existencia = Existencia.enCero(negocioId, producto.getId(), bodegaId);
        existencia.aplicar(1, new BigDecimal("3"));
        when(existencias.findByProductoIdOrderByBodegaId(producto.getId()))
                .thenReturn(List.of(existencia));

        ResultadoDeBusqueda resultado = busqueda.buscar(new FiltroDeBusqueda(null, null, false, null));

        assertThat(resultado.productos().get(0).stockTotal()).isEqualByComparingTo(new BigDecimal("3"));
    }
}
