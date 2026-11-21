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
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
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
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-169 · Contado o crédito al registrar la factura (I22 a I25, I29). Factura de referencia:
 * A 10 × $ 10.000 y B 5 × $ 20.000, IVA 19 %, flete $ 10.000 → total $ 248.000, fechada el 2026-11-20.
 */
class ContadoOCreditoTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ProveedorProductoRepositorio catalogo = mock(ProveedorProductoRepositorio.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final GestionDeCuentasPorPagar cuentasPorPagar = new GestionDeCuentasPorPagar(cuentas, pagos,
            recepciones, proveedores, caja, eventos);
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas,
            mock(OrdenDeCompraRepositorio.class), mock(OrdenCompraLineaRepositorio.class), cuentas, proveedores,
            catalogo, consecutivos, eventos, inventario, null, cuentasPorPagar, caja);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private Proveedor proveedor;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private final List<CuentaPorPagar> cuentasGuardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        proveedor = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, new BigDecimal("1000000"), null, null);
        when(proveedores.findByIdAndNegocioId(proveedor.getId(), negocioId)).thenReturn(Optional.of(proveedor));
        when(inventario.bodegaActiva(negocioId, bodegaId)).thenReturn(true);
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(9L);
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoParaComprar(a, "A", "Acetaminofén", true, "IVA_19", new BigDecimal("19"), false, false),
                b, new ProductoParaComprar(b, "B", "Ibuprofeno", true, "IVA_19", new BigDecimal("19"), false, false)));
        when(catalogo.findByProveedorId(proveedor.getId())).thenReturn(List.of());
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

    private SolicitudDeCompraDirecta factura(String condicion, String metodo, Boolean desdeLaCaja) {
        return new SolicitudDeCompraDirecta(proveedor.getId(), bodegaId, "FV-100", LocalDate.of(2026, 11, 20),
                new BigDecimal("10000"), null, null, List.of(
                        new SolicitudDeCompraDirecta.LineaDeCompra(a, BigDecimal.TEN, new BigDecimal("10000"),
                                null, null, null, null, null),
                        new SolicitudDeCompraDirecta.LineaDeCompra(b, new BigDecimal("5"), new BigDecimal("20000"),
                                null, null, null, null, null)),
                condicion, metodo, desdeLaCaja, null);
    }

    private RecepcionDelNegocio registrarYConfirmar(SolicitudDeCompraDirecta solicitud) {
        gestion.crearDirecta(solicitud);
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        Recepcion recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(List.copyOf(guardadas));
        return gestion.confirmar(recepcion.getId(), null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> pagoPublicado() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("PagoProveedor"), any(), eq("pago_a_proveedor_registrado"),
                payload.capture());
        return (Map<String, Object>) payload.getValue();
    }

    @Test
    @DisplayName("HU-169 C1 · A crédito con un proveedor de 30 días: cuenta por $ 248.000 que vence el 2026-12-20")
    void aCredito() {
        RecepcionDelNegocio r = registrarYConfirmar(factura("CREDITO", null, null));

        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        assertThat(cuenta.getMonto()).isEqualByComparingTo("248000");
        assertThat(cuenta.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 20));
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(r.condicionPago()).isEqualTo("CREDITO");
        verify(pagos, never()).save(any());
    }

    @Test
    @DisplayName("HU-169 · Sin condición indicada, la compra es a crédito")
    void porOmisionACredito() {
        RecepcionDelNegocio r = registrarYConfirmar(factura(null, null, null));

        assertThat(r.condicionPago()).isEqualTo("CREDITO");
        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
    }

    @Test
    @DisplayName("HU-169 C2 · De contado en efectivo desde la caja: la cuenta nace PAGADA y la caja recibe la salida")
    void contadoDesdeLaCaja() {
        registrarYConfirmar(factura("CONTADO", "EFECTIVO", true));

        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
        assertThat(cuenta.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 11, 20));
        ArgumentCaptor<PagoProveedor> pago = ArgumentCaptor.forClass(PagoProveedor.class);
        verify(pagos).save(pago.capture());
        assertThat(pago.getValue().getMonto()).isEqualByComparingTo("248000");
        assertThat(pago.getValue().getSesionCajaId()).isEqualTo(sesion);
        Map<String, Object> e = pagoPublicado();
        assertThat(e.get("sesion_caja_id")).isEqualTo(sesion.toString());
        assertThat(e.get("metodo")).isEqualTo("EFECTIVO");
    }

    @Test
    @DisplayName("HU-169 C3 · De contado por transferencia: la cuenta nace PAGADA y la caja no cambia")
    void contadoPorTransferencia() {
        registrarYConfirmar(factura("CONTADO", "TRANSFERENCIA", null));

        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
        assertThat(pagoPublicado().get("sesion_caja_id")).isNull();
        verify(caja, never()).sesionAbiertaDe(any(), any());
    }

    @Test
    @DisplayName("HU-169 C3 · De contado en efectivo pero con dinero de fuera de la caja: la caja no cambia")
    void contadoEfectivoDeFuera() {
        registrarYConfirmar(factura("CONTADO", "EFECTIVO", false));

        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
        assertThat(pagoPublicado().get("sesion_caja_id")).isNull();
    }

    @Test
    @DisplayName("HU-145 C4 · Pagar desde la caja sin tener una caja abierta: 409 y no se registra la compra")
    void sinCajaAbierta() {
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestion.crearDirecta(factura("CONTADO", "EFECTIVO", true)))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("No tienes una caja abierta");
        verify(recepciones, never()).save(any());
    }

    @Test
    @DisplayName("HU-169 · De contado sin método de pago se rechaza (422)")
    void contadoSinMetodo() {
        assertThatThrownBy(() -> gestion.crearDirecta(factura("CONTADO", null, null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Indica con qué se pagó");
    }

    @Test
    @DisplayName("HU-169 · Solo el efectivo sale de la caja (422)")
    void soloEfectivoDesdeLaCaja() {
        assertThatThrownBy(() -> gestion.crearDirecta(factura("CONTADO", "TARJETA", true)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Solo un pago en efectivo");
    }

    @Test
    @DisplayName("HU-169 C4 · A crédito por encima del cupo: se avisa «Esta compra supera el cupo del proveedor.» y se registra")
    void superaElCupo() {
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.<Object[]>of(
                new Object[] { proveedor.getId(), new BigDecimal("800000") }));

        RecepcionDelNegocio r = registrarYConfirmar(factura("CREDITO", null, null));

        assertThat(r.avisos()).containsExactly("Esta compra supera el cupo del proveedor.");
        assertThat(r.estado()).isEqualTo("CONFIRMADA");
        assertThat(cuentasGuardadas).hasSize(1);
    }

    @Test
    @DisplayName("HU-169 C4 · Dentro del cupo, o de contado, no hay aviso")
    void dentroDelCupo() {
        RecepcionDelNegocio credito = gestion.crearDirecta(factura("CREDITO", null, null));
        assertThat(credito.avisos()).isEmpty();

        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.<Object[]>of(
                new Object[] { proveedor.getId(), new BigDecimal("900000") }));
        RecepcionDelNegocio contado = gestion.crearDirecta(new SolicitudDeCompraDirecta(proveedor.getId(),
                bodegaId, "FV-200", null, null, null, null, factura(null, null, null).lineas(),
                "CONTADO", "TRANSFERENCIA", null, "TRX-1"));
        assertThat(contado.avisos()).isEmpty();
    }

    @Test
    @DisplayName("HU-169 C5 · Vencida ayer: aparece vencida con 1 día de mora y su estado guardado sigue PENDIENTE")
    void vencidaCalculada() {
        LocalDate hoy = LocalDate.now();
        CuentaPorPagar c = CuentaPorPagar.abrirPorRecepcion(negocioId, proveedor.getId(), null, "FV-1",
                new BigDecimal("100000"), hoy.minusDays(31), 30);
        when(cuentas.findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId)).thenReturn(List.of(c));

        CuentaDelNegocio vista = cuentasPorPagar.listar(false).get(0);

        assertThat(vista.vencida()).isTrue();
        assertThat(vista.diasDeMora()).isEqualTo(1);
        assertThat(vista.estado()).isEqualTo("PENDIENTE");
    }
}
