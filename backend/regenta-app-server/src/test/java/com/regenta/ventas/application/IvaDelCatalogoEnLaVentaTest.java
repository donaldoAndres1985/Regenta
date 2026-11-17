package com.regenta.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.PrecioDeCatalogo;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.ventas.domain.CotizacionLinea;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infrastructure.AsignadorDeConsecutivos;
import com.regenta.ventas.infrastructure.CotizacionLineaRepositorio;
import com.regenta.ventas.infrastructure.CotizacionRepositorio;
import com.regenta.ventas.infrastructure.VentaLineaRepositorio;
import com.regenta.ventas.infrastructure.VentaRepositorio;

/** HU-148 · el backend decide precio e IVA de cada línea desde el catálogo (I3, I4). */
class IvaDelCatalogoEnLaVentaTest {

    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final VentaLineaRepositorio lineas = mock(VentaLineaRepositorio.class);
    private final ConfiguracionDelNegocio configuracion = mock(ConfiguracionDelNegocio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final PrecioDeLinea precios = new PrecioDeLinea(inventario, configuracion);

    private final GestionDeVentas gestion = new GestionDeVentas(ventas, lineas,
            mock(AsignadorDeConsecutivos.class), mock(SagaDeConfirmacionDeVenta.class),
            configuracion, mock(com.regenta.caja.api.CajaApi.class), precios);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();
    private Venta venta;

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(),
                Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_COTIZACION_CREAR"), Set.of()));
        venta = Venta.crear(negocioId, UUID.randomUUID(), "1", null, UUID.randomUUID(), null, "MOSTRADOR");
        when(ventas.findById(venta.getId())).thenReturn(Optional.of(venta));
        when(lineas.findByVentaIdOrderByLinea(any())).thenReturn(List.of());
        when(configuracion.responsableDeIva(negocioId)).thenReturn(true);
        catalogo("35500", "IVA_19", "19");
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void catalogo(String precio, String tarifa, String pct) {
        when(inventario.precioDeCatalogo(negocioId, productoId)).thenReturn(Optional.of(
                new PrecioDeCatalogo(new BigDecimal(precio), tarifa, new BigDecimal(pct))));
    }

    /** Lo que mandaría un POS desactualizado o malintencionado: otro precio y sin IVA. */
    private SolicitudDeLinea delPos(String cantidad) {
        return new SolicitudDeLinea(productoId, "7707031403512", "Barniz galón", "GLN",
                new BigDecimal(cantidad), new BigDecimal("1000"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, null);
    }

    private SolicitudDeLinea fraccion(String cantidad, String medida) {
        return new SolicitudDeLinea(productoId, "PIN-R", "Pintulux rojo galón", "GLN",
                new BigDecimal(cantidad), new BigDecimal("1"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, medida);
    }

    private void conFracciones() {
        when(inventario.fraccionesDe(negocioId, productoId)).thenReturn(List.of(
                new com.regenta.inventario.api.FraccionDeCatalogo("¼ galón", new BigDecimal("0.25"),
                        new BigDecimal("18000"))));
    }

    @Test
    @DisplayName("HU-158 C3 · 3 de ¼ de galón cobra 3 × $ 18.000 = $ 54.000 con el IVA del producto y descuenta 0,75")
    void venderFracciones() {
        conFracciones();

        LineaDeVenta l = gestion.agregarLinea(venta.getId(), fraccion("3", "¼ galón"));

        assertThat(l.cantidad()).isEqualByComparingTo("0.75");
        assertThat(l.total()).isEqualByComparingTo("54000");
        assertThat(l.impuestoPct()).isEqualByComparingTo("19");
        assertThat(l.impuestoValor()).isEqualByComparingTo("8621.85");
        assertThat(l.nombre()).isEqualTo("Pintulux rojo galón (¼ galón)");
    }

    @Test
    @DisplayName("HU-158 C4 · El precio de la fracción lo toma el backend del catálogo, no del POS")
    void elPrecioDeLaFraccionEsDelCatalogo() {
        conFracciones();

        LineaDeVenta l = gestion.agregarLinea(venta.getId(), fraccion("1", "¼ galón"));

        assertThat(l.total()).isEqualByComparingTo("18000");
    }

    @Test
    @DisplayName("HU-158 · Una medida que el producto no tiene no entra a la venta")
    void fraccionQueNoExiste() {
        conFracciones();

        assertThatThrownBy(() -> gestion.agregarLinea(venta.getId(), fraccion("1", "⅛ galón")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("HU-148 C1 · La línea guarda IVA_19, base $ 29.831,93 e IVA $ 5.668,07 aunque el POS mande otro precio y 0 %")
    void elIvaLoDecideElCatalogo() {
        LineaDeVenta l = gestion.agregarLinea(venta.getId(), delPos("1"));

        assertThat(l.precioUnitario()).isEqualByComparingTo("35500");
        assertThat(l.impuestoPct()).isEqualByComparingTo("19");
        assertThat(l.baseGravable()).isEqualByComparingTo("29831.93");
        assertThat(l.impuestoValor()).isEqualByComparingTo("5668.07");
        assertThat(l.impuestoCodigo()).isEqualTo("IVA_19");
    }

    @Test
    @DisplayName("HU-148 C2 · Un negocio no responsable de IVA vende con IVA $ 0 y base $ 35.500")
    void sinResponsabilidadNoHayIva() {
        when(configuracion.responsableDeIva(negocioId)).thenReturn(false);

        LineaDeVenta l = gestion.agregarLinea(venta.getId(), delPos("1"));

        assertThat(l.impuestoValor()).isEqualByComparingTo("0");
        assertThat(l.baseGravable()).isEqualByComparingTo("35500");
        assertThat(l.total()).isEqualByComparingTo("35500");
    }

    @Test
    @DisplayName("HU-148 C4 · Si después cambia el precio del producto, la línea ya creada no cambia")
    void laLineaCopiaElPrecio() {
        gestion.agregarLinea(venta.getId(), delPos("1"));
        ArgumentCaptor<VentaLinea> guardada = ArgumentCaptor.forClass(VentaLinea.class);
        Mockito.verify(lineas).save(guardada.capture());
        when(lineas.findByVentaIdAndLinea(venta.getId(), (short) 1))
                .thenReturn(Optional.of(guardada.getValue()));
        catalogo("40000", "IVA_5", "5");

        gestion.cambiarCantidad(venta.getId(), (short) 1, new BigDecimal("2"));

        assertThat(guardada.getValue().getTotal()).isEqualByComparingTo("71000");
        assertThat(guardada.getValue().getImpuestoPct()).isEqualByComparingTo("19");
    }

    @Test
    @DisplayName("HU-148 · Un producto que no está en el catálogo del negocio no entra a la venta")
    void sinProductoEnElCatalogoSeRechaza() {
        when(inventario.precioDeCatalogo(negocioId, productoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestion.agregarLinea(venta.getId(), delPos("1")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("HU-148 C5 · La cotización toma precio e IVA del catálogo igual que la venta")
    void laCotizacionTomaElCatalogo() {
        CotizacionLineaRepositorio lineasCot = mock(CotizacionLineaRepositorio.class);
        AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
        when(consecutivos.siguiente(any(), any())).thenReturn(7L);
        GestionDeCotizaciones cotizaciones = new GestionDeCotizaciones(mock(CotizacionRepositorio.class),
                lineasCot, consecutivos, gestion, mock(ConsultaDeClientes.class), precios);

        cotizaciones.crear(new SolicitudDeCotizacion(null, LocalDate.now().plusDays(5),
                List.of(new LineaDeCotizacion(productoId, "7707031403512", "Barniz galón", "GLN",
                        BigDecimal.ONE, new BigDecimal("1000"), BigDecimal.ZERO, null,
                        BigDecimal.ZERO, BigDecimal.ZERO))));
        // (LineaDeCotizacion no lleva fracción: se cotiza por unidad entera.)

        ArgumentCaptor<CotizacionLinea> guardada = ArgumentCaptor.forClass(CotizacionLinea.class);
        Mockito.verify(lineasCot).save(guardada.capture());
        assertThat(guardada.getValue().getPrecioUnitario()).isEqualByComparingTo("35500");
        assertThat(guardada.getValue().getImpuestoPct()).isEqualByComparingTo("19");
        assertThat(guardada.getValue().getTotal()).isEqualByComparingTo("35500");
    }

    @Test
    @DisplayName("HU-148 C6 · Una venta con varias tarifas muestra base e IVA por tarifa")
    void losTotalesSeDesglosanPorTarifa() {
        VentaLinea a = VentaLinea.de(negocioId, venta.getId(), (short) 1, productoId, "A", "Barniz",
                "GLN", BigDecimal.ONE, new BigDecimal("35500"), BigDecimal.ZERO, "IVA_19",
                new BigDecimal("19"), BigDecimal.ZERO);
        VentaLinea b = VentaLinea.de(negocioId, venta.getId(), (short) 2, UUID.randomUUID(), "B",
                "Arroz", "UND", BigDecimal.ONE, new BigDecimal("4000"), BigDecimal.ZERO, "EXCLUIDO",
                BigDecimal.ZERO, BigDecimal.ZERO);
        when(lineas.findByVentaIdOrderByLinea(venta.getId())).thenReturn(List.of(a, b));

        VentaDelNegocio dto = gestion.ver(venta.getId());

        assertThat(dto.impuestosPorTarifa()).extracting(ImpuestoPorTarifa::codigo)
                .containsExactly("IVA_19", "EXCLUIDO");
        assertThat(dto.impuestosPorTarifa().get(0).base()).isEqualByComparingTo("29831.93");
        assertThat(dto.impuestosPorTarifa().get(0).iva()).isEqualByComparingTo("5668.07");
        assertThat(dto.impuestosPorTarifa().get(1).base()).isEqualByComparingTo("4000");
        assertThat(dto.impuestosPorTarifa().get(1).iva()).isEqualByComparingTo("0");
    }
}
