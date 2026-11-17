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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-153 · inventario general con filtros y valor (I2, I6, I8, I10). */
class InventarioGeneralTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);
    private final CategoriaRepositorio categorias = mock(CategoriaRepositorio.class);
    private final InventarioGeneral inventario =
            new InventarioGeneral(productos, existencias, categorias);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID principal = UUID.randomUUID();
    private final UUID bodega2 = UUID.randomUUID();
    private final UUID pinturas = UUID.randomUUID();
    private final UUID herramientas = UUID.randomUUID();

    private Producto barniz;
    private Producto martillo;
    private Producto viejo;

    private Producto producto(String sku, String nombre, UUID categoria, String precio, String costo) {
        Producto p = Producto.nuevo(negocioId, sku, nombre, categoria, UUID.randomUUID(),
                TarifaIva.IVA_19, UUID.randomUUID());
        p.datosBasicos(null, null, null, new BigDecimal(precio), null);
        p.registrarCostoDeEntrada(new BigDecimal(costo));
        return p;
    }

    private Existencia en(Producto p, UUID bodega, String cantidad) {
        Existencia e = Existencia.enCero(negocioId, p.getId(), bodega);
        BigDecimal c = new BigDecimal(cantidad);
        e.aplicar(c.signum() < 0 ? -1 : 1, c.abs());
        return e;
    }

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_EXISTENCIA_VER"), Set.of()));
        barniz = producto("BAR-1", "Barniz galón transparente", pinturas, "35500", "26500");
        martillo = producto("MAR-1", "Martillo de uña", herramientas, "20000", "12000");
        viejo = producto("VIE-1", "Barniz viejo descontinuado", pinturas, "10000", "5000");
        viejo.desactivar();
        martillo.cambiarTarifa(TarifaIva.IVA_19);
        when(productos.findByNegocioIdOrderByNombreAsc(negocioId))
                .thenReturn(List.of(barniz, martillo, viejo));
        when(existencias.findByNegocioId(negocioId)).thenReturn(List.of(
                en(barniz, principal, "2"), en(barniz, bodega2, "1"),
                en(martillo, principal, "-2"),
                en(viejo, principal, "4")));
        Categoria cPinturas = mock(Categoria.class);
        when(cPinturas.getId()).thenReturn(pinturas);
        when(cPinturas.getNombre()).thenReturn("Pinturas");
        when(categorias.findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(any()))
                .thenReturn(List.of(cPinturas));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private FiltroDeInventario filtro() {
        return new FiltroDeInventario(null, null, null, false, false, null, 0, 50);
    }

    @Test
    @DisplayName("HU-153 C1 · Cada fila trae SKU, nombre, categoría, tarifa, existencia, costo, precio y su valor a costo y a precio")
    void cadaFilaTraeSuValor() {
        InventarioDelNegocio r = inventario.consultar(filtro());

        FilaDeInventario fila = r.filas().stream().filter(f -> f.sku().equals("BAR-1")).findFirst().orElseThrow();
        assertThat(fila.categoria()).isEqualTo("Pinturas");
        assertThat(fila.tarifaIva()).isEqualTo(TarifaIva.IVA_19);
        assertThat(fila.existencia()).isEqualByComparingTo("3");
        assertThat(fila.costo()).isEqualByComparingTo("26500");
        assertThat(fila.valorCosto()).isEqualByComparingTo("79500");
        assertThat(fila.valorPrecio()).isEqualByComparingTo("106500");
    }

    @Test
    @DisplayName("HU-153 C2 · Por defecto se ven los activos; con «inactivos» solo los desactivados")
    void filtroPorEstado() {
        assertThat(inventario.consultar(filtro()).filas()).extracting(FilaDeInventario::sku)
                .containsExactly("BAR-1", "MAR-1");
        assertThat(inventario.consultar(new FiltroDeInventario(null, null, "INACTIVOS", false, false,
                null, 0, 50)).filas()).extracting(FilaDeInventario::sku).containsExactly("VIE-1");
    }

    @Test
    @DisplayName("HU-153 C2 · Los filtros «tarifa por revisar», «existencia negativa» y categoría, con los totales sobre lo filtrado")
    void filtrosYTotales() {
        // Como lo dejó la migración de HU-147 (I6): el barniz con la tarifa sin confirmar.
        org.springframework.test.util.ReflectionTestUtils.setField(barniz, "tarifaPorRevisar", true);
        InventarioDelNegocio porRevisar = inventario.consultar(
                new FiltroDeInventario(null, null, null, true, false, null, 0, 50));
        assertThat(porRevisar.filas()).extracting(FilaDeInventario::sku).containsExactly("BAR-1");

        InventarioDelNegocio negativos = inventario.consultar(
                new FiltroDeInventario(null, null, null, false, true, null, 0, 50));
        assertThat(negativos.filas()).extracting(FilaDeInventario::sku).containsExactly("MAR-1");

        InventarioDelNegocio deCategoria = inventario.consultar(
                new FiltroDeInventario(null, pinturas, null, false, false, null, 0, 50));
        assertThat(deCategoria.filas()).extracting(FilaDeInventario::sku).containsExactly("BAR-1");
        assertThat(deCategoria.valorCosto()).isEqualByComparingTo("79500");
        assertThat(deCategoria.valorPrecio()).isEqualByComparingTo("106500");
    }

    @Test
    @DisplayName("HU-153 C3 · Varias palabras encuentran los productos que las contienen todas, en cualquier orden")
    void busquedaPorPalabras() {
        InventarioDelNegocio r = inventario.consultar(
                new FiltroDeInventario("transparente BARNIZ", null, null, false, false, null, 0, 50));

        assertThat(r.filas()).extracting(FilaDeInventario::sku).containsExactly("BAR-1");
    }

    @Test
    @DisplayName("HU-153 C4 · Con una bodega elegida, existencia y valores son los de esa bodega")
    void porBodega() {
        InventarioDelNegocio r = inventario.consultar(
                new FiltroDeInventario(null, null, null, false, false, bodega2, 0, 50));

        FilaDeInventario fila = r.filas().stream().filter(f -> f.sku().equals("BAR-1")).findFirst().orElseThrow();
        assertThat(fila.existencia()).isEqualByComparingTo("1");
        assertThat(fila.valorCosto()).isEqualByComparingTo("26500");
    }

    @Test
    @DisplayName("HU-153 C1 · El listado viene paginado y dice cuántas filas hay en total")
    void paginado() {
        InventarioDelNegocio r = inventario.consultar(
                new FiltroDeInventario(null, null, null, false, false, null, 1, 1));

        assertThat(r.filas()).extracting(FilaDeInventario::sku).containsExactly("MAR-1");
        assertThat(r.totalFilas()).isEqualTo(2);
    }
}
