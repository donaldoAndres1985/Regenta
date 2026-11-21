package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.PagoProveedor;
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
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.inventario.api.ReglaDeLote;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-175 · Recibir contra una orden. Orden de referencia: A 10 × $ 10.000 y B 5 × $ 20.000, IVA 19 %;
 * con flete de $ 10.000 la factura suma base $ 200.000 · IVA $ 38.000 · total $ 248.000.
 */
class RecibirContraOrdenTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio ordenLineas = mock(OrdenCompraLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ConfiguracionDeComprasRepositorio configuraciones = mock(ConfiguracionDeComprasRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final GestionDeCuentasPorPagar cuentasPorPagar = new GestionDeCuentasPorPagar(cuentas, pagos, recepciones,
            proveedores, caja, eventos, mock(AplicacionDePagoRepositorio.class));
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas, ordenes,
            ordenLineas, cuentas, proveedores, mock(ProveedorProductoRepositorio.class), consecutivos, eventos,
            inventario, null, cuentasPorPagar, caja, new GestionDeConfiguracionDeCompras(configuraciones));

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea lineaA;
    private OrdenCompraLinea lineaB;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private final List<CuentaPorPagar> cuentasGuardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        orden = OrdenDeCompra.crear(negocioId, "OC-1", p.getId(), UUID.randomUUID(), null, UUID.randomUUID(), null,
                BigDecimal.ZERO, null);
        lineaA = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, a, "Acetaminofén", BigDecimal.TEN,
                new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        lineaB = OrdenCompraLinea.nueva(orden.getId(), negocioId, 2, b, "Ibuprofeno", new BigDecimal("5"),
                new BigDecimal("20000"), BigDecimal.ZERO, new BigDecimal("19"));
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaA, lineaB));
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(7L);
        when(configuraciones.findById(negocioId)).thenReturn(Optional.empty()); // tolerancia 5 %
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ReglaDeLote(a, false, false), b, new ReglaDeLote(b, false, false)));
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoParaComprar(a, "A", "Acetaminofén", true, "IVA_19", new BigDecimal("19"), false, false),
                b, new ProductoParaComprar(b, "B", "Ibuprofeno", true, "IVA_19", new BigDecimal("19"), false, false)));
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.of());
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.of(sesion));
        when(caja.sesionAbierta(negocioId, sesion)).thenReturn(true);
        when(recepcionLineas.saveAll(any())).thenAnswer(i -> {
            guardadas.clear();
            i.<Iterable<RecepcionLinea>>getArgument(0).forEach(guardadas::add);
            return guardadas;
        });
        when(cuentas.save(any())).thenAnswer(i -> {
            CuentaPorPagar c = i.getArgument(0);
            if (!cuentasGuardadas.contains(c)) {
                cuentasGuardadas.add(c);
            }
            return c;
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void enviada() {
        orden.aprobar(UUID.randomUUID(), true);
        orden.enviar();
    }

    private SolicitudDeRecepcion.LineaDeRecepcion de(OrdenCompraLinea l, String cantidad) {
        return new SolicitudDeRecepcion.LineaDeRecepcion(l.getId(), new BigDecimal(cantidad), null, false, null, null,
                null);
    }

    private RecepcionDelNegocio recibirYConfirmar(SolicitudDeRecepcion solicitud) {
        clearInvocations(recepciones);
        gestion.crear(solicitud);
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        Recepcion recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(List.copyOf(guardadas));
        return gestion.confirmar(recepcion.getId(), null);
    }

    private SolicitudDeRecepcion factura(String numero, String condicion, String metodo, Boolean desdeLaCaja,
            List<SolicitudDeRecepcion.LineaDeRecepcion> lineas) {
        return new SolicitudDeRecepcion(orden.getId(), null, numero, null, lineas, LocalDate.of(2026, 11, 20),
                new BigDecimal("10000"), null, condicion, metodo, desdeLaCaja, null);
    }

    @Test
    @DisplayName("C1 · De una orden ENVIADA de 10 und llegan 6: queda PARCIAL, faltan 4 y admite otra recepción")
    void parcial() {
        enviada();

        recibirYConfirmar(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null, List.of(de(lineaA, "6"))));

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.PARCIAL);
        assertThat(lineaA.faltante()).isEqualByComparingTo("4");
        orden.exigirRecibible();
    }

    @Test
    @DisplayName("C2 · Con lo que faltaba, la orden queda RECIBIDA")
    void completa() {
        enviada();
        recibirYConfirmar(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null,
                List.of(de(lineaA, "6"), de(lineaB, "5"))));

        recibirYConfirmar(new SolicitudDeRecepcion(orden.getId(), null, "FV-2", null, List.of(de(lineaA, "4"))));

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        assertThat(lineaA.faltante()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("C3 · De una orden en BORRADOR no se recibe: 409")
    void borrador() {
        assertThatThrownBy(() -> gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null,
                List.of(de(lineaA, "1")))))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("BORRADOR");
        verify(recepciones, never()).save(any());
    }

    @Test
    @DisplayName("C3 · De una orden ANULADA no se recibe: 409")
    void anulada() {
        ReflectionTestUtils.setField(orden, "estado", EstadoOrdenCompra.ANULADA);

        assertThatThrownBy(() -> gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null,
                List.of(de(lineaA, "1")))))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("ANULADA");
    }

    @Test
    @DisplayName("C4 · A crédito, como la compra directa: cuenta por $ 248.000 con el flete, a 30 días, y costo de entrada con el flete repartido")
    void aCreditoComoLaCompraDirecta() {
        enviada();

        RecepcionDelNegocio r = recibirYConfirmar(factura("FV-100", "CREDITO", null, null,
                List.of(de(lineaA, "10"), de(lineaB, "5"))));

        assertThat(r.total()).isEqualByComparingTo("248000");
        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        assertThat(cuenta.getMonto()).isEqualByComparingTo("248000");
        assertThat(cuenta.getNumeroFactura()).isEqualTo("FV-100");
        assertThat(cuenta.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        // $ 10.000 de flete sobre $ 200.000 de base: 5 % a cada unidad (I14).
        assertThat(guardadas).extracting(RecepcionLinea::getCostoEntrada)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("10500"), new BigDecimal("21000"));
        assertThat(guardadas).extracting(RecepcionLinea::getTarifaIva).containsOnly("IVA_19");
    }

    @Test
    @DisplayName("C4 · De contado en efectivo desde la caja: la cuenta nace PAGADA y sale de la sesión abierta")
    void deContadoDesdeLaCaja() {
        enviada();

        recibirYConfirmar(factura("FV-100", "CONTADO", "EFECTIVO", true, List.of(de(lineaA, "10"), de(lineaB, "5"))));

        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
        ArgumentCaptor<PagoProveedor> pago = ArgumentCaptor.forClass(PagoProveedor.class);
        verify(pagos).save(pago.capture());
        assertThat(pago.getValue().getMonto()).isEqualByComparingTo("248000");
        assertThat(pago.getValue().getSesionCajaId()).isEqualTo(sesion);
    }

    @Test
    @DisplayName("C4 · Una factura ya registrada para el proveedor se rechaza también contra orden: 409")
    void facturaRepetida() {
        enviada();
        when(cuentas.existsByNegocioIdAndProveedorIdAndNumeroFactura(negocioId, orden.getProveedorId(), "FV-100"))
                .thenReturn(true);

        assertThatThrownBy(() -> gestion.crear(factura("FV-100", null, null, null, List.of(de(lineaA, "1")))))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("FV-100");
    }

    @Test
    @DisplayName("C5 · 106 de 100 pedidas con tolerancia 5 %: se rechaza la recepción entera (422) y no se guarda nada")
    void fueraDeTolerancia() {
        enviada();
        OrdenCompraLinea cien = OrdenCompraLinea.nueva(orden.getId(), negocioId, 3, a, "Acetaminofén",
                new BigDecimal("100"), new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaB, cien));

        assertThatThrownBy(() -> gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null,
                List.of(de(lineaB, "5"), de(cien, "106")))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tolerancia de 5 %");
        verify(recepciones, never()).save(any());
        verify(recepcionLineas, never()).saveAll(any());
        assertThat(cien.getCantidadRecibida()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("C5 · La misma línea repetida suma para la tolerancia: 60 + 50 de 100 se rechaza")
    void lineaRepetidaSuma() {
        enviada();
        OrdenCompraLinea cien = OrdenCompraLinea.nueva(orden.getId(), negocioId, 3, a, "Acetaminofén",
                new BigDecimal("100"), new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(cien));

        assertThatThrownBy(() -> gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-1", null,
                List.of(de(cien, "60"), de(cien, "50")))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
