package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.NivelStock;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/** HU-035. Búsqueda de productos y resolución de código de barras. */
class BusquedaDeProductosTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_PRODUCTO_VER", "INVENTARIO_BODEGA_CREAR");

    @Autowired
    private BusquedaDeProductos busqueda;

    @Autowired
    private LibroMayorDeInventario libro;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID cemento;
    private UUID broca;
    private UUID bodega;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID herramientas = categorias
                    .crear(new SolicitudDeCategoria("Herramientas", null, null, null, null, null))
                    .id();
            UUID plomeria = categorias
                    .crear(new SolicitudDeCategoria("Plomeria", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);

            cemento = productos.crear(new SolicitudDeProducto("CEM-050", "7701234567890",
                    "Cemento gris 50 kg", null, plomeria, unidad, null, null, new BigDecimal("32000"),
                    true, new BigDecimal("20"), null, false, false, false, false, null)).id();
            productos.crear(new SolicitudDeProducto("PVC-012", null, "Tubo PVC 1/2\" x 6 m", null,
                    plomeria, unidad, null, null, new BigDecimal("14000"), true, BigDecimal.ZERO,
                    null, false, false, false, false, null)).id();
            broca = productos.crear(new SolicitudDeProducto("BRO-SDS12", null, "Broca SDS 12 mm",
                    null, herramientas, unidad, null, null, new BigDecimal("15000"), true,
                    BigDecimal.ZERO, null, false, false, false, false, null)).id();

            bodega = bodegas.crear(new SolicitudDeBodega("B1", "Bodega Centro", null, null)).id();
            libro.registrar(new SolicitudDeMovimiento(cemento, bodega, TipoMovimiento.ENTRADA_COMPRA,
                    new BigDecimal("10"), OrigenMovimiento.CARGA_INICIAL, null, "carga", "c-cem"));
            // broca queda en 0

            busqueda.agregarCodigo(cemento, new SolicitudDeCodigo("CAJACEM12", new BigDecimal("12"),
                    "Caja x 12"));
            return null;
        });
    }

    private ResultadoDeBusqueda buscar(String q) {
        return enContexto(negocio, usuario, SETUP,
                () -> busqueda.buscar(new FiltroDeBusqueda(q, null, false, null)));
    }

    @Test
    @DisplayName("Criterio 1: la búsqueda filtra por nombre, SKU o código de barras")
    void filtraPorNombreSkuOCodigo() {
        assertThat(buscar("cem").productos()).extracting(ProductoEncontrado::sku)
                .containsExactly("CEM-050");
        assertThat(buscar("PVC").productos()).extracting(ProductoEncontrado::sku)
                .containsExactly("PVC-012");
        assertThat(buscar("770123").productos()).extracting(ProductoEncontrado::sku)
                .containsExactly("CEM-050");
        assertThat(buscar("cajacem").productos()).extracting(ProductoEncontrado::sku)
                .containsExactly("CEM-050");
    }

    @Test
    @DisplayName("Criterio 1: con menos de tres caracteres la búsqueda se rechaza")
    void menosDeTresCaracteres() {
        assertThatThrownBy(() -> buscar("ce"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tres");
    }

    @Test
    @DisplayName("Sin término se listan todos los productos activos")
    void sinTerminoListaTodos() {
        assertThat(buscar(null).productos()).hasSize(3);
        assertThat(buscar(null).total()).isEqualTo(3);
    }

    @Test
    @DisplayName("El resultado trae el nivel de stock: NORMAL, BAJO o CERO")
    void nivelDeStock() {
        assertThat(buscar("cemento").productos()).singleElement()
                .satisfies(p -> assertThat(p.nivelStock()).isEqualTo(NivelStock.BAJO));
        assertThat(buscar("broca").productos()).singleElement()
                .satisfies(p -> assertThat(p.nivelStock()).isEqualTo(NivelStock.CERO));
    }

    @Test
    @DisplayName("Criterio 4: un código alterno se resuelve al producto con su factor de conversión")
    void codigoAlternoResuelveConFactor() {
        CodigoResuelto r = enContexto(negocio, usuario, SETUP,
                () -> busqueda.resolverCodigo("CAJACEM12"));

        assertThat(r.productoId()).isEqualTo(cemento);
        assertThat(r.factor()).isEqualByComparingTo("12");
        assertThat(r.esAlterno()).isTrue();
    }

    @Test
    @DisplayName("Criterio 2/4: el código de barras principal se resuelve con factor 1")
    void codigoPrincipalResuelveConFactorUno() {
        CodigoResuelto r = enContexto(negocio, usuario, SETUP,
                () -> busqueda.resolverCodigo("7701234567890"));

        assertThat(r.productoId()).isEqualTo(cemento);
        assertThat(r.factor()).isEqualByComparingTo("1");
        assertThat(r.esAlterno()).isFalse();
    }

    @Test
    @DisplayName("Un código que no existe no se resuelve")
    void codigoInexistente() {
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP,
                () -> busqueda.resolverCodigo("NADA-QUE-VER")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Un código alterno repetido en el negocio se rechaza")
    void codigoAlternoRepetido() {
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> {
            busqueda.agregarCodigo(broca, new SolicitudDeCodigo("CAJACEM12", new BigDecimal("6"),
                    "otra"));
            return null;
        })).isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("La búsqueda de otro negocio no ve estos productos")
    void aisladoPorNegocio() {
        assertThat(enContexto(UUID.randomUUID(), usuario, SETUP,
                () -> busqueda.buscar(new FiltroDeBusqueda(null, null, false, null)).productos()))
                .isEmpty();
    }
}
