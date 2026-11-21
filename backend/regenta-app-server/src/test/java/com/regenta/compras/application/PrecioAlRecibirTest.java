package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AplicacionDePagoRepositorio;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.ConfiguracionDeComprasRepositorio;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.CostoYPrecio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.inventario.api.ReglaDeLote;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/**
 * HU-179 · Precio de venta al recibir (I20). A tiene costo promedio $ 9.000 y precio $ 14.280 (IVA 19 %,
 * base $ 12.000); en la factura de referencia (A 10 × $ 10.000, B 5 × $ 20.000, flete $ 10.000) entra a
 * $ 10.500.
 */
class PrecioAlRecibirTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio ordenLineas = mock(OrdenCompraLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas, ordenes,
            ordenLineas, cuentas, proveedores, mock(ProveedorProductoRepositorio.class), consecutivos, eventos,
            inventario, null,
            new GestionDeCuentasPorPagar(cuentas, mock(PagoProveedorRepositorio.class), recepciones, proveedores,
                    caja, eventos, mock(AplicacionDePagoRepositorio.class)),
            caja, new GestionDeConfiguracionDeCompras(mock(ConfiguracionDeComprasRepositorio.class)));

    private final UUID negocioId = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private OrdenDeCompra orden;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private Recepcion recepcion;

    @BeforeEach
    void datos() {
        conPermisos("COMPRAS_COMPRA_CREAR", "INVENTARIO_PRODUCTO_EDITAR");
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        orden = OrdenDeCompra.crear(negocioId, "OC-1", p.getId(), UUID.randomUUID(), null, UUID.randomUUID(), null,
                BigDecimal.ZERO, null);
        orden.aprobar(UUID.randomUUID(), true);
        orden.enviar();
        OrdenCompraLinea lineaA = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, a, "Acetaminofén",
                BigDecimal.TEN, new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        OrdenCompraLinea lineaB = OrdenCompraLinea.nueva(orden.getId(), negocioId, 2, b, "Ibuprofeno",
                new BigDecimal("5"), new BigDecimal("20000"), BigDecimal.ZERO, new BigDecimal("19"));
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaA, lineaB));
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(1L);
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ReglaDeLote(a, false, false), b, new ReglaDeLote(b, false, false)));
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoParaComprar(a, "A", "Acetaminofén", true, "IVA_19", new BigDecimal("19"), false, false),
                b, new ProductoParaComprar(b, "B", "Ibuprofeno", true, "IVA_19", new BigDecimal("19"), false, false)));
        when(inventario.costosYPrecios(eq(negocioId), any(), anyCollection())).thenReturn(Map.of(
                a, new CostoYPrecio(a, new BigDecimal("9000"), new BigDecimal("14280"), new BigDecimal("12000"),
                        "IVA_19"),
                b, new CostoYPrecio(b, new BigDecimal("21000"), new BigDecimal("29750"), new BigDecimal("25000"),
                        "IVA_19")));
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.of());
        when(recepcionLineas.saveAll(any())).thenAnswer(i -> {
            List<RecepcionLinea> nuevas = new ArrayList<>();
            i.<Iterable<RecepcionLinea>>getArgument(0).forEach(nuevas::add);
            if (guardadas.isEmpty()) {
                guardadas.addAll(nuevas);
            }
            return nuevas;
        });

        gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-100", null, List.of(
                new SolicitudDeRecepcion.LineaDeRecepcion(lineaA.getId(), BigDecimal.TEN, null, false, null, null, null),
                new SolicitudDeRecepcion.LineaDeRecepcion(lineaB.getId(), new BigDecimal("5"), null, false, null, null,
                        null)),
                LocalDate.of(2026, 11, 20), new BigDecimal("10000"), null, null, null, null, null));
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenAnswer(i -> List.copyOf(guardadas));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(permisos), Set.of()));
    }

    private SolicitudDeConfirmacionDeRecepcion conPrecioDeA(String precio) {
        return new SolicitudDeConfirmacionDeRecepcion(null,
                List.of(new SolicitudDeConfirmacionDeRecepcion.PrecioNuevo(a, new BigDecimal(precio))));
    }

    @Test
    @DisplayName("C1 · La línea muestra costo $ 9.000 → $ 10.500 y margen 25,0 % → 12,5 % sobre la base de $ 12.000")
    void margen() {
        MargenDeLinea linea = gestion.margenes(recepcion.getId()).stream()
                .filter(m -> m.productoId().equals(a)).findFirst().orElseThrow();

        assertThat(linea.costoAnterior()).isEqualByComparingTo("9000");
        assertThat(linea.costoNuevo()).isEqualByComparingTo("10500");
        assertThat(linea.precioVenta()).isEqualByComparingTo("14280");
        assertThat(linea.base()).isEqualByComparingTo("12000");
        assertThat(linea.margenAnterior()).isEqualByComparingTo("25.0");
        assertThat(linea.margenNuevo()).isEqualByComparingTo("12.5");
        assertThat(linea.cambiaElCosto()).isTrue();
    }

    @Test
    @DisplayName("C2 · Con precio nuevo $ 15.470, al confirmar el producto queda en $ 15.470 con IVA incluido")
    void precioNuevo() {
        gestion.confirmar(recepcion.getId(), conPrecioDeA("15470"));

        verify(inventario).cambiarPrecioDeVenta(negocioId, a, new BigDecimal("15470"));
        verify(inventario, never()).cambiarPrecioDeVenta(any(), eq(b), any());
        assertThat(guardadas.get(0).getPrecioVentaNuevo()).isEqualByComparingTo("15470");
        assertThat(gestion.margenes(recepcion.getId()).get(0).precioVentaNuevo()).isEqualByComparingTo("15470");
    }

    @Test
    @DisplayName("C3 · Sin precio nuevo, al confirmar el precio del producto no cambia")
    void sinPrecioNuevo() {
        gestion.confirmar(recepcion.getId(), null);

        verify(inventario, never()).cambiarPrecioDeVenta(any(), any(), any());
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.CONFIRMADA);
    }

    @Test
    @DisplayName("C4 · Sin INVENTARIO_PRODUCTO_EDITAR, un precio nuevo es 403 y la recepción no se confirma; el margen sí se ve")
    void sinPermiso() {
        conPermisos("COMPRAS_COMPRA_CREAR", "COMPRAS_COMPRA_VER");

        assertThat(gestion.margenes(recepcion.getId())).hasSize(2);
        assertThatThrownBy(() -> gestion.confirmar(recepcion.getId(), conPrecioDeA("15470")))
                .isInstanceOf(SinPermisoException.class);
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.BORRADOR);
        verify(inventario, never()).cambiarPrecioDeVenta(any(), any(), any());
    }

    @Test
    @DisplayName("Un precio de un producto que no llegó en la recepción es 422")
    void productoAjeno() {
        assertThatThrownBy(() -> gestion.confirmar(recepcion.getId(),
                new SolicitudDeConfirmacionDeRecepcion(null, List.of(
                        new SolicitudDeConfirmacionDeRecepcion.PrecioNuevo(UUID.randomUUID(), BigDecimal.TEN)))))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.BORRADOR);
    }

    @Test
    @DisplayName("El margen se calcula sobre la base sin IVA: (12.000 − 10.500) ÷ 12.000 = 12,5 %")
    void formula() {
        assertThat(MargenDeLinea.margen(new BigDecimal("12000"), new BigDecimal("10500"))).isEqualByComparingTo("12.5");
        assertThat(MargenDeLinea.margen(BigDecimal.ZERO, BigDecimal.ONE)).isNull();
    }
}
