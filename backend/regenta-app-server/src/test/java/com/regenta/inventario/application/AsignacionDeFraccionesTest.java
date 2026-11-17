package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TablaDeFracciones;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.TablaDeFraccionesRepositorio;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.SinPermisoException;

/** HU-156 · crear tablas de fracciones y asignarles productos (I12-I14). */
class AsignacionDeFraccionesTest {

    private final TablaDeFraccionesRepositorio tablas = mock(TablaDeFraccionesRepositorio.class);
    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final UUID negocioId = UUID.randomUUID();
    private TablaDeFracciones pintulux;
    private List<Producto> colores;

    private GestionDeFracciones gestion(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(permisos), Set.of()));
        AspectJProxyFactory fabrica = new AspectJProxyFactory(new GestionDeFracciones(tablas, productos,
                mock(com.regenta.inventario.infrastructure.PrecioPropioDeFraccionRepositorio.class)));
        fabrica.addAspect(new GuardiaDePermisos());
        return fabrica.getProxy();
    }

    private static List<SolicitudDeTablaDeFracciones.Linea> lineas(String precioCuarto) {
        return List.of(
                new SolicitudDeTablaDeFracciones.Linea("½ galón", new BigDecimal("0.5"), new BigDecimal("34000")),
                new SolicitudDeTablaDeFracciones.Linea("¾ galón", new BigDecimal("0.75"), new BigDecimal("48000")),
                new SolicitudDeTablaDeFracciones.Linea("¼ galón", new BigDecimal("0.25"), new BigDecimal(precioCuarto)),
                new SolicitudDeTablaDeFracciones.Linea("⅛ galón", new BigDecimal("0.125"), new BigDecimal("10000")));
    }

    @BeforeEach
    void datos() {
        pintulux = TablaDeFracciones.nueva(negocioId, "Esmalte Pintulux galón", List.of(
                new TablaDeFracciones.Definicion("¼ galón", new BigDecimal("0.25"), new BigDecimal("18000"))));
        when(tablas.findById(pintulux.getId())).thenReturn(Optional.of(pintulux));
        when(tablas.save(any())).thenAnswer(i -> i.getArgument(0));
        colores = new ArrayList<>();
        IntStream.range(0, 20).forEach(i -> {
            Producto p = Producto.nuevo(negocioId, "PIN-" + i, "Pintulux color " + i + " galón",
                    UUID.randomUUID(), UUID.randomUUID(), TarifaIva.IVA_19, UUID.randomUUID());
            colores.add(p);
            when(productos.findById(p.getId())).thenReturn(Optional.of(p));
        });
        when(productos.findAllById(any())).thenAnswer(i -> {
            List<UUID> ids = new ArrayList<>();
            ((Iterable<UUID>) i.getArgument(0)).forEach(ids::add);
            return colores.stream().filter(p -> ids.contains(p.getId())).toList();
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private List<UUID> ids() {
        return colores.stream().map(Producto::getId).toList();
    }

    @Test
    @DisplayName("HU-156 C1 · Se crea la tabla con sus cuatro fracciones y precios exactos")
    void seCreaLaTabla() {
        TablaDeFraccionesDelNegocio t = gestion("INVENTARIO_FRACCION_GESTIONAR").crearTabla(
                new SolicitudDeTablaDeFracciones("Esmalte Pintulux galón", lineas("18000")));

        assertThat(t.fracciones()).extracting(FraccionDelNegocio::precio)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("34000"), new BigDecimal("48000"),
                        new BigDecimal("18000"), new BigDecimal("10000"));
    }

    @Test
    @DisplayName("HU-156 · Un nombre de tabla repetido se rechaza")
    void nombreRepetido() {
        when(tablas.existsByNegocioIdAndNombreIgnoreCase(negocioId, "Esmalte Pintulux galón")).thenReturn(true);

        assertThatThrownBy(() -> gestion("INVENTARIO_FRACCION_GESTIONAR").crearTabla(
                new SolicitudDeTablaDeFracciones("Esmalte Pintulux galón", lineas("18000"))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("HU-156 C3 · Los 20 resultados de la búsqueda se asignan a la tabla y quedan con sus fracciones")
    void asignaLos20() {
        int asignados = gestion("INVENTARIO_FRACCION_GESTIONAR")
                .asignar(pintulux.getId(), new AsignacionDeTabla(ids(), false));

        assertThat(asignados).isEqualTo(20);
        assertThat(colores).allSatisfy(p -> assertThat(p.getTablaFraccionesId()).isEqualTo(pintulux.getId()));
        assertThat(gestion("INVENTARIO_PRODUCTO_VER").fraccionesDe(colores.get(7).getId()))
                .extracting(FraccionDelNegocio::medida).containsExactly("¼ galón");
    }

    @Test
    @DisplayName("HU-156 C4 · Al cambiar el ¼ a $ 19.000 en la tabla, los 20 cobran $ 19.000")
    void cambiarElPrecioDeLaTablaLoCambiaParaTodos() {
        gestion("INVENTARIO_FRACCION_GESTIONAR").asignar(pintulux.getId(), new AsignacionDeTabla(ids(), false));

        gestion("INVENTARIO_FRACCION_GESTIONAR").editarTabla(pintulux.getId(),
                new SolicitudDeTablaDeFracciones("Esmalte Pintulux galón", lineas("19000")));

        for (Producto p : colores) {
            assertThat(gestion("INVENTARIO_PRODUCTO_VER").fraccionesDe(p.getId()))
                    .filteredOn(f -> f.medida().equals("¼ galón"))
                    .singleElement().extracting(FraccionDelNegocio::precio)
                    .satisfies(precio -> assertThat(precio).isEqualByComparingTo("19000"));
        }
    }

    @Test
    @DisplayName("HU-156 C5 · Un producto que ya tiene otra tabla pide confirmar el cambio")
    void cambioDeTablaPideConfirmacion() {
        UUID otra = UUID.randomUUID();
        colores.get(0).asignarTablaDeFracciones(otra);

        assertThatThrownBy(() -> gestion("INVENTARIO_FRACCION_GESTIONAR")
                .asignar(pintulux.getId(), new AsignacionDeTabla(ids(), false)))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("1 de estos productos ya tiene otra tabla");
        assertThat(colores.get(1).getTablaFraccionesId()).isNull();

        gestion("INVENTARIO_FRACCION_GESTIONAR").asignar(pintulux.getId(), new AsignacionDeTabla(ids(), true));
        assertThat(colores.get(0).getTablaFraccionesId()).isEqualTo(pintulux.getId());
    }

    @Test
    @DisplayName("HU-156 C6 · No se pueden asignar productos de otro negocio")
    void productosAjenos() {
        List<UUID> conAjeno = new ArrayList<>(ids());
        conAjeno.add(UUID.randomUUID());

        assertThatThrownBy(() -> gestion("INVENTARIO_FRACCION_GESTIONAR")
                .asignar(pintulux.getId(), new AsignacionDeTabla(conAjeno, false)))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(colores).allSatisfy(p -> assertThat(p.getTablaFraccionesId()).isNull());
    }

    @Test
    @DisplayName("HU-156 · Sin INVENTARIO_FRACCION_GESTIONAR no se crean tablas (403)")
    void sinPermiso() {
        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR").crearTabla(
                new SolicitudDeTablaDeFracciones("X", lineas("18000"))))
                .isInstanceOf(SinPermisoException.class);
    }
}
