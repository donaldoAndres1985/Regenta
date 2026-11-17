package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.domain.PrecioPropioDeFraccion;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TablaDeFracciones;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.infrastructure.PrecioPropioDeFraccionRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.TablaDeFraccionesRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-157 · el precio propio de una fracción le gana al de la tabla (I14). */
class PrecioPropioDeFraccionTest {

    private final TablaDeFraccionesRepositorio tablas = mock(TablaDeFraccionesRepositorio.class);
    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final PrecioPropioDeFraccionRepositorio propios = mock(PrecioPropioDeFraccionRepositorio.class);
    private final List<PrecioPropioDeFraccion> guardados = new ArrayList<>();
    private final GestionDeFracciones gestion = new GestionDeFracciones(tablas, productos, propios);

    private final UUID negocioId = UUID.randomUUID();
    private TablaDeFracciones pintulux;
    private Producto blanco;
    private Producto rojo;

    private Producto color(String nombre) {
        Producto p = Producto.nuevo(negocioId, nombre, nombre, UUID.randomUUID(), UUID.randomUUID(),
                TarifaIva.IVA_19, UUID.randomUUID());
        p.asignarTablaDeFracciones(pintulux.getId());
        when(productos.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private static List<TablaDeFracciones.Definicion> cuarto(String precio) {
        return List.of(new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal(precio)));
    }

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_FRACCION_GESTIONAR",
                        "INVENTARIO_PRODUCTO_VER"), Set.of()));
        pintulux = TablaDeFracciones.nueva(negocioId, "Esmalte Pintulux galón", cuarto("18000"));
        when(tablas.findById(pintulux.getId())).thenReturn(Optional.of(pintulux));
        blanco = color("Pintulux blanco galón");
        rojo = color("Pintulux rojo galón");
        when(propios.save(any())).thenAnswer(i -> {
            PrecioPropioDeFraccion p = i.getArgument(0);
            guardados.removeIf(g -> g.getProductoId().equals(p.getProductoId()) && g.getMedida().equals(p.getMedida()));
            guardados.add(p);
            return p;
        });
        when(propios.findByProductoId(any())).thenAnswer(i -> guardados.stream()
                .filter(g -> g.getProductoId().equals(i.getArgument(0))).toList());
        when(propios.findByProductoIdAndMedida(any(), any())).thenAnswer(i -> guardados.stream()
                .filter(g -> g.getProductoId().equals(i.getArgument(0)) && g.getMedida().equals(i.getArgument(1)))
                .findFirst());
        doAnswer(i -> guardados.remove(i.<PrecioPropioDeFraccion>getArgument(0))).when(propios).delete(any());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private BigDecimal precioDelCuarto(Producto p) {
        return gestion.fraccionesDe(p.getId()).stream().filter(f -> f.medida().equals("¼ galón"))
                .findFirst().orElseThrow().precio();
    }

    @Test
    @DisplayName("HU-157 C1 · El blanco con precio propio de ¼ a $ 20.000 cobra eso; el rojo sigue en $ 18.000")
    void elPropioLeGanaALaTabla() {
        gestion.fijarPrecioPropio(blanco.getId(), "¼ galón", new BigDecimal("20000"));

        assertThat(precioDelCuarto(blanco)).isEqualByComparingTo("20000");
        assertThat(precioDelCuarto(rojo)).isEqualByComparingTo("18000");
    }

    @Test
    @DisplayName("HU-157 C2 · Si cambia el ¼ de la tabla, el blanco sigue con su precio propio")
    void cambiarLaTablaNoTocaElPropio() {
        gestion.fijarPrecioPropio(blanco.getId(), "¼ galón", new BigDecimal("20000"));

        pintulux.reemplazarFracciones(cuarto("19000"));

        assertThat(precioDelCuarto(blanco)).isEqualByComparingTo("20000");
        assertThat(precioDelCuarto(rojo)).isEqualByComparingTo("19000");
    }

    @Test
    @DisplayName("HU-157 C3 · Quitar el precio propio vuelve al de la tabla")
    void quitarVuelveALaTabla() {
        gestion.fijarPrecioPropio(blanco.getId(), "¼ galón", new BigDecimal("20000"));

        gestion.quitarPrecioPropio(blanco.getId(), "¼ galón");

        assertThat(precioDelCuarto(blanco)).isEqualByComparingTo("18000");
    }

    @Test
    @DisplayName("HU-157 C4 · Cada fracción dice si su precio viene de la tabla o es propio")
    void dicenSuOrigen() {
        gestion.fijarPrecioPropio(blanco.getId(), "¼ galón", new BigDecimal("20000"));

        assertThat(gestion.fraccionesDe(blanco.getId())).extracting(FraccionDelNegocio::origen)
                .containsExactly("PROPIO");
        assertThat(gestion.fraccionesDe(rojo.getId())).extracting(FraccionDelNegocio::origen)
                .containsExactly("TABLA");
    }

    @Test
    @DisplayName("HU-157 · No se pone precio propio a una medida que no está en la tabla del producto")
    void soloMedidasDeLaTabla() {
        assertThatThrownBy(() -> gestion.fijarPrecioPropio(blanco.getId(), "⅛ galón", new BigDecimal("9000")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
