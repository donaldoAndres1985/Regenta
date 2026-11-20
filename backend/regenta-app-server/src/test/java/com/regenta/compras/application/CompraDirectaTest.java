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

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;

/**
 * HU-167 · Compra directa con la factura del proveedor. Factura de referencia: A, 10 und × $ 10.000;
 * B, 5 und × $ 20.000; ambas IVA_19; flete $ 10.000 → base $ 200.000, IVA $ 38.000, total $ 248.000.
 */
class CompraDirectaTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ProveedorProductoRepositorio catalogo = mock(ProveedorProductoRepositorio.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas,
            mock(OrdenDeCompraRepositorio.class), mock(OrdenCompraLineaRepositorio.class), cuentas,
            proveedores, catalogo, consecutivos, eventos, inventario, null);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final UUID productoA = UUID.randomUUID();
    private final UUID productoB = UUID.randomUUID();
    private Proveedor proveedor;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        proveedor = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO,
                null, null);
        when(proveedores.findByIdAndNegocioId(proveedor.getId(), negocioId)).thenReturn(Optional.of(proveedor));
        when(inventario.bodegaActiva(negocioId, bodegaId)).thenReturn(true);
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(7L);
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                productoA, producto(productoA, "Acetaminofén 500 mg", true),
                productoB, producto(productoB, "Ibuprofeno 400 mg", true)));
        when(catalogo.findByProveedorId(proveedor.getId())).thenReturn(List.of());
        when(recepcionLineas.saveAll(any())).thenAnswer(i -> {
            guardadas.clear();
            i.<Iterable<RecepcionLinea>>getArgument(0).forEach(guardadas::add);
            return guardadas;
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private static ProductoParaComprar producto(UUID id, String nombre, boolean activo) {
        return new ProductoParaComprar(id, "SKU-" + nombre.length(), nombre, activo, "IVA_19",
                new BigDecimal("19"), false, false);
    }

    private SolicitudDeCompraDirecta.LineaDeCompra linea(UUID producto, String cantidad, String costo) {
        return new SolicitudDeCompraDirecta.LineaDeCompra(producto, new BigDecimal(cantidad),
                costo == null ? null : new BigDecimal(costo), null, null, null, null, null);
    }

    private SolicitudDeCompraDirecta facturaDeReferencia(String numero) {
        return new SolicitudDeCompraDirecta(proveedor.getId(), bodegaId, numero,
                LocalDate.of(2026, 11, 20), new BigDecimal("10000"), null, null,
                List.of(linea(productoA, "10", "10000"), linea(productoB, "5", "20000")));
    }

    /** Hace que la recepción recién creada se pueda volver a leer para confirmarla. */
    private Recepcion crearYRecordar(SolicitudDeCompraDirecta solicitud) {
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        gestion.crearDirecta(solicitud);
        verify(recepciones, atLeastOnce()).save(r.capture());
        Recepcion recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(List.copyOf(guardadas));
        return recepcion;
    }

    @Test
    @DisplayName("HU-167 C1 · Sin orden: base $ 200.000, IVA $ 38.000, flete $ 10.000 y total $ 248.000")
    void facturaDeReferencia() {
        RecepcionDelNegocio r = gestion.crearDirecta(facturaDeReferencia("FV-100"));

        assertThat(r.ordenId()).isNull();
        assertThat(r.compraDirecta()).isTrue();
        assertThat(r.estado()).isEqualTo("BORRADOR");
        assertThat(r.numero()).isEqualTo("REC-7");
        assertThat(r.subtotal()).isEqualByComparingTo("200000");
        assertThat(r.impuestoTotal()).isEqualByComparingTo("38000");
        assertThat(r.flete()).isEqualByComparingTo("10000");
        assertThat(r.total()).isEqualByComparingTo("248000");
        assertThat(r.fechaFactura()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    @DisplayName("HU-167 C1 · Al confirmar, la cuenta por pagar nace por $ 248.000 desde la fecha de la factura")
    void cuentaPorElTotal() {
        Recepcion recepcion = crearYRecordar(facturaDeReferencia("FV-100"));

        gestion.confirmar(recepcion.getId(), null);

        ArgumentCaptor<CuentaPorPagar> c = ArgumentCaptor.forClass(CuentaPorPagar.class);
        verify(cuentas).save(c.capture());
        assertThat(c.getValue().getMonto()).isEqualByComparingTo("248000");
        assertThat(c.getValue().getFechaEmision()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(c.getValue().getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 20));
        verify(eventos).registrar(eq(negocioId), eq("Recepcion"), eq(recepcion.getId()),
                eq("recepcion_registrada"), any());
    }

    @Test
    @DisplayName("HU-167 C2 · A un proveedor desactivado no se le compra (422)")
    void proveedorDesactivado() {
        proveedor.desactivar();

        assertThatThrownBy(() -> gestion.crearDirecta(facturaDeReferencia("FV-100")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("desactivado");
        verify(recepciones, never()).save(any());
    }

    @Test
    @DisplayName("HU-167 C3 · La tarifa de IVA viene del catálogo de Inventario")
    void tarifaDelCatalogo() {
        RecepcionDelNegocio r = gestion.crearDirecta(facturaDeReferencia("FV-100"));

        assertThat(r.lineas()).allSatisfy(l -> {
            assertThat(l.tarifaIva()).isEqualTo("IVA_19");
            assertThat(l.impuestoPct()).isEqualByComparingTo("19");
        });
        assertThat(r.lineas().get(0).nombre()).isEqualTo("Acetaminofén 500 mg");
    }

    @Test
    @DisplayName("HU-167 C3 · Sin costo escrito, se usa el último costo del catálogo del proveedor")
    void costoSugerido() {
        when(catalogo.findByProveedorId(proveedor.getId())).thenReturn(List.of(ProveedorProducto.de(
                negocioId, proveedor.getId(), productoA, "AND-77", new BigDecimal("9800"), null, null, false)));

        RecepcionDelNegocio r = gestion.crearDirecta(new SolicitudDeCompraDirecta(proveedor.getId(),
                bodegaId, "FV-101", null, null, null, null, List.of(linea(productoA, "10", null))));

        assertThat(r.lineas().get(0).costoUnitario()).isEqualByComparingTo("9800");
    }

    @Test
    @DisplayName("HU-167 C3 · Sin costo y sin compras anteriores a ese proveedor, se pide el costo (422)")
    void sinCostoNiCatalogo() {
        assertThatThrownBy(() -> gestion.crearDirecta(new SolicitudDeCompraDirecta(proveedor.getId(),
                bodegaId, "FV-101", null, null, null, null, List.of(linea(productoA, "10", null)))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Indica el costo");
    }

    @Test
    @DisplayName("HU-167 C4 · Al confirmar, el catálogo queda con el costo y el código de esta factura; lo nuevo se agrega")
    void catalogoActualizado() {
        ProveedorProducto conocido = ProveedorProducto.de(negocioId, proveedor.getId(), productoA, "VIEJO",
                new BigDecimal("9500"), null, null, true);
        when(catalogo.findByProveedorIdAndProductoId(proveedor.getId(), productoA)).thenReturn(Optional.of(conocido));
        when(catalogo.findByProveedorIdAndProductoId(proveedor.getId(), productoB)).thenReturn(Optional.empty());
        Recepcion recepcion = crearYRecordar(new SolicitudDeCompraDirecta(proveedor.getId(), bodegaId,
                "FV-100", null, null, null, null, List.of(
                        new SolicitudDeCompraDirecta.LineaDeCompra(productoA, BigDecimal.TEN,
                                new BigDecimal("10000"), null, "AND-77", null, null, null),
                        linea(productoB, "5", "20000"))));

        gestion.confirmar(recepcion.getId(), null);

        assertThat(conocido.getCostoUltimo()).isEqualByComparingTo("10000");
        assertThat(conocido.getCodigoProveedor()).isEqualTo("AND-77");
        assertThat(conocido.isPreferido()).isTrue();
        ArgumentCaptor<ProveedorProducto> guardados = ArgumentCaptor.forClass(ProveedorProducto.class);
        verify(catalogo, atLeastOnce()).save(guardados.capture());
        assertThat(guardados.getAllValues()).anySatisfy(pp -> {
            assertThat(pp.getProductoId()).isEqualTo(productoB);
            assertThat(pp.getCostoUltimo()).isEqualByComparingTo("20000");
            assertThat(pp.isPreferido()).isFalse();
        });
    }

    @Test
    @DisplayName("HU-167 C5 · Una compra confirmada no se edita (409)")
    void confirmadaNoSeEdita() {
        Recepcion recepcion = crearYRecordar(facturaDeReferencia("FV-100"));
        gestion.confirmar(recepcion.getId(), null);

        assertThatThrownBy(() -> gestion.actualizarDirecta(recepcion.getId(), facturaDeReferencia("FV-100")))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-167 C5 · En borrador sí se corrige: cambian las líneas y el total")
    void borradorSeCorrige() {
        Recepcion recepcion = crearYRecordar(facturaDeReferencia("FV-100"));

        RecepcionDelNegocio r = gestion.actualizarDirecta(recepcion.getId(), new SolicitudDeCompraDirecta(
                proveedor.getId(), bodegaId, "FV-100", null, null, null, null, List.of(linea(productoA, "2", "10000"))));

        assertThat(r.lineas()).hasSize(1);
        assertThat(r.total()).isEqualByComparingTo("23800");
    }

    @Test
    @DisplayName("HU-167 C6 · Una factura ya registrada para el mismo proveedor responde 409")
    void facturaRepetida() {
        when(cuentas.existsByNegocioIdAndProveedorIdAndNumeroFactura(negocioId, proveedor.getId(), "FV-100"))
                .thenReturn(true);

        assertThatThrownBy(() -> gestion.crearDirecta(facturaDeReferencia(" FV-100 ")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("FV-100");
    }

    @Test
    @DisplayName("HU-167 C6 · Otra compra en borrador con la misma factura también cuenta")
    void facturaEnOtroBorrador() {
        Recepcion otra = Recepcion.crear(negocioId, "REC-1", null, proveedor.getId(), bodegaId, "FV-100",
                null, null);
        when(recepciones.findByNegocioIdAndProveedorIdAndFacturaProveedor(negocioId, proveedor.getId(), "FV-100"))
                .thenReturn(List.of(otra));

        assertThatThrownBy(() -> gestion.crearDirecta(facturaDeReferencia("FV-100")))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-167 · Sin número de factura no hay compra directa (422)")
    void sinFactura() {
        assertThatThrownBy(() -> gestion.crearDirecta(facturaDeReferencia("  ")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("número de la factura");
    }

    @Test
    @DisplayName("HU-167 · Una bodega que no es del negocio o está inactiva responde 404")
    void bodegaAjena() {
        when(inventario.bodegaActiva(negocioId, bodegaId)).thenReturn(false);

        assertThatThrownBy(() -> gestion.crearDirecta(facturaDeReferencia("FV-100")))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("HU-167 · Un producto desactivado no se compra (422)")
    void productoDesactivado() {
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                productoA, producto(productoA, "Acetaminofén 500 mg", false),
                productoB, producto(productoB, "Ibuprofeno 400 mg", true)));

        assertThatThrownBy(() -> gestion.crearDirecta(facturaDeReferencia("FV-100")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("desactivado");
    }

    @Test
    @DisplayName("HU-167 · Un descuento de línea del 10 % baja la base y el IVA en proporción")
    void descuentoDeLinea() {
        RecepcionDelNegocio r = gestion.crearDirecta(new SolicitudDeCompraDirecta(proveedor.getId(), bodegaId,
                "FV-102", null, null, null, null, List.of(new SolicitudDeCompraDirecta.LineaDeCompra(productoA,
                        BigDecimal.TEN, new BigDecimal("10000"), BigDecimal.TEN, null, null, null, null))));

        assertThat(r.subtotal()).isEqualByComparingTo("90000");
        assertThat(r.impuestoTotal()).isEqualByComparingTo("17100");
        assertThat(r.total()).isEqualByComparingTo("107100");
    }

    // ---------------------------------------------------------------- HU-168 · costo de entrada

    private GestionDeRecepciones conRegimen(boolean responsable) {
        ConfiguracionNegocioApi configuracion = mock(ConfiguracionNegocioApi.class);
        when(configuracion.responsableDeIva(negocioId)).thenReturn(responsable);
        return new GestionDeRecepciones(recepciones, recepcionLineas, mock(OrdenDeCompraRepositorio.class),
                mock(OrdenCompraLineaRepositorio.class), cuentas, proveedores, catalogo, consecutivos, eventos,
                inventario, configuracion);
    }

    /** Confirma la compra con [g] y devuelve el costo unitario que se le manda a Inventario por línea. */
    @SuppressWarnings("unchecked")
    private List<BigDecimal> costosEnviadosAInventario(GestionDeRecepciones g, SolicitudDeCompraDirecta compra) {
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        g.crearDirecta(compra);
        verify(recepciones, atLeastOnce()).save(r.capture());
        Recepcion recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(List.copyOf(guardadas));
        g.confirmar(recepcion.getId(), null);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("Recepcion"), eq(recepcion.getId()),
                eq("recepcion_registrada"), payload.capture());
        List<Map<String, Object>> lineas = (List<Map<String, Object>>) ((Map<String, Object>) payload.getValue())
                .get("lineas");
        return lineas.stream().map(l -> (BigDecimal) l.get("costo_unitario")).toList();
    }

    @Test
    @DisplayName("HU-168 C1 · Negocio responsable de IVA: Inventario recibe A a $ 10.500 y B a $ 21.000")
    void inventarioRecibeCostoResponsable() {
        List<BigDecimal> costos = costosEnviadosAInventario(conRegimen(true), facturaDeReferencia("FV-100"));

        assertThat(costos.get(0)).isEqualByComparingTo("10500");
        assertThat(costos.get(1)).isEqualByComparingTo("21000");
    }

    @Test
    @DisplayName("HU-168 C2 · Negocio no responsable: Inventario recibe A a $ 12.400 y B a $ 24.800")
    void inventarioRecibeCostoNoResponsable() {
        List<BigDecimal> costos = costosEnviadosAInventario(conRegimen(false), facturaDeReferencia("FV-100"));

        assertThat(costos.get(0)).isEqualByComparingTo("12400");
        assertThat(costos.get(1)).isEqualByComparingTo("24800");
    }

    @Test
    @DisplayName("HU-168 C2 · El régimen no cambia lo que se le debe al proveedor: la factura sigue en $ 248.000")
    void elRegimenNoCambiaLaDeuda() {
        RecepcionDelNegocio r = conRegimen(false).crearDirecta(facturaDeReferencia("FV-100"));

        assertThat(r.total()).isEqualByComparingTo("248000");
        assertThat(r.lineas().get(0).costoEntrada()).isEqualByComparingTo("12400");
    }

    @Test
    @DisplayName("HU-168 C4 · Con $ 4.000 de descuento global la base baja a $ 196.000 y el IVA a $ 37.240")
    void descuentoGlobalEnLaFactura() {
        RecepcionDelNegocio r = conRegimen(true).crearDirecta(new SolicitudDeCompraDirecta(proveedor.getId(),
                bodegaId, "FV-103", null, new BigDecimal("10000"), new BigDecimal("4000"), null,
                List.of(linea(productoA, "10", "10000"), linea(productoB, "5", "20000"))));

        assertThat(r.subtotal()).isEqualByComparingTo("200000");
        assertThat(r.descuentoGlobal()).isEqualByComparingTo("4000");
        assertThat(r.impuestoTotal()).isEqualByComparingTo("37240");
        assertThat(r.total()).isEqualByComparingTo("243240");
        assertThat(r.lineas().get(0).costoEntrada()).isEqualByComparingTo("10300");
        assertThat(r.lineas().get(1).costoEntrada()).isEqualByComparingTo("20600");
    }

    @Test
    @DisplayName("HU-168 · Un descuento global mayor que la factura se rechaza (422)")
    void descuentoGlobalExcesivo() {
        assertThatThrownBy(() -> conRegimen(true).crearDirecta(new SolicitudDeCompraDirecta(proveedor.getId(),
                bodegaId, "FV-104", null, null, new BigDecimal("300000"), null,
                List.of(linea(productoA, "10", "10000")))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("descuento global");
    }
}
