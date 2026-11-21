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
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-170 · Recibir con remisión y registrar la factura después (I22, I23). */
class RemisionTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final GestionDeCuentasPorPagar cuentasPorPagar = new GestionDeCuentasPorPagar(cuentas, pagos,
            recepciones, proveedores, caja, eventos);
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas,
            mock(OrdenDeCompraRepositorio.class), mock(OrdenCompraLineaRepositorio.class), cuentas, proveedores,
            mock(ProveedorProductoRepositorio.class), consecutivos, eventos, inventario, null, cuentasPorPagar, caja);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private Proveedor proveedor;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private final List<CuentaPorPagar> cuentasGuardadas = new ArrayList<>();
    private Recepcion recepcion;

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        proveedor = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(proveedor.getId(), negocioId)).thenReturn(Optional.of(proveedor));
        when(proveedores.findAllById(any())).thenReturn(List.of(proveedor));
        when(inventario.bodegaActiva(negocioId, bodegaId)).thenReturn(true);
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(12L);
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoParaComprar(a, "A", "Acetaminofén", true, "IVA_19", new BigDecimal("19"), false, false)));
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.of());
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
            when(cuentas.findByIdAndNegocioId(c.getId(), negocioId)).thenReturn(Optional.of(c));
            return c;
        });
        when(pagos.findByCuentaIdOrderByFechaAsc(any())).thenReturn(List.of());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private SolicitudDeCompraDirecta conRemision(String condicion, String metodo) {
        return new SolicitudDeCompraDirecta(proveedor.getId(), bodegaId, null, null, null, null, "Remisión R-88",
                List.of(new SolicitudDeCompraDirecta.LineaDeCompra(a, BigDecimal.TEN, new BigDecimal("10000"),
                        null, null, null, null, null)),
                condicion, metodo, null, null);
    }

    private RecepcionDelNegocio recibirConRemision() {
        gestion.crearDirecta(conRemision("CREDITO", null));
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        recepcion = r.getValue();
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepciones.findAllById(any())).thenReturn(List.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(List.copyOf(guardadas));
        when(cuentas.findByNegocioIdAndRecepcionId(negocioId, recepcion.getId()))
                .thenAnswer(i -> List.copyOf(cuentasGuardadas));
        return gestion.confirmar(recepcion.getId(), null);
    }

    @Test
    @DisplayName("HU-170 C1 · A crédito sin número de factura: el inventario entra y la cuenta queda en espera de factura")
    void entraYQuedaEnEspera() {
        RecepcionDelNegocio r = recibirConRemision();

        assertThat(r.estado()).isEqualTo("CONFIRMADA");
        verify(eventos).registrar(eq(negocioId), eq("Recepcion"), eq(recepcion.getId()), eq("recepcion_registrada"), any());
        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        assertThat(cuenta.enEsperaDeFactura()).isTrue();
        assertThat(cuenta.getNumeroFactura()).isNull();
        assertThat(cuenta.getMonto()).isEqualByComparingTo("119000");
        assertThat(cuenta.getFechaVencimiento()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("HU-170 C1 · A una cuenta en espera de factura ya se le puede abonar")
    void seAbonaEnEspera() {
        recibirConRemision();
        CuentaPorPagar cuenta = cuentasGuardadas.get(0);

        cuentasPorPagar.registrarPago(cuenta.getId(), new SolicitudDePago(new BigDecimal("19000"), "TRANSFERENCIA",
                null, null));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("100000");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PARCIAL);
        assertThat(cuenta.enEsperaDeFactura()).isTrue();
    }

    @Test
    @DisplayName("HU-170 C2 · Sin número de factura no se registra una compra de contado (422)")
    void contadoSinFactura() {
        assertThatThrownBy(() -> gestion.crearDirecta(conRemision("CONTADO", "TRANSFERENCIA")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("contado necesita el número de la factura");
        verify(recepciones, never()).save(any());
    }

    @Test
    @DisplayName("HU-170 C3 · Al registrar número y fecha, se quita la marca y el vencimiento sale de la fecha de la factura")
    void llegaLaFactura() {
        recibirConRemision();
        CuentaPorPagar cuenta = cuentasGuardadas.get(0);

        CuentaDelNegocio vista = cuentasPorPagar.registrarFacturaDeCuenta(cuenta.getId(),
                new SolicitudDeFacturaDeCuenta(" FV-300 ", LocalDate.of(2026, 11, 25)));

        assertThat(vista.enEsperaDeFactura()).isFalse();
        assertThat(vista.numeroFactura()).isEqualTo("FV-300");
        assertThat(vista.fechaEmision()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(vista.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(recepcion.getFacturaProveedor()).isEqualTo("FV-300");
        assertThat(recepcion.getFechaFactura()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    @DisplayName("HU-170 C3 · Una cuenta que ya tiene factura no se vuelve a facturar (409)")
    void yaTieneFactura() {
        recibirConRemision();
        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        cuentasPorPagar.registrarFacturaDeCuenta(cuenta.getId(),
                new SolicitudDeFacturaDeCuenta("FV-300", LocalDate.of(2026, 11, 25)));

        assertThatThrownBy(() -> cuentasPorPagar.registrarFacturaDeCuenta(cuenta.getId(),
                new SolicitudDeFacturaDeCuenta("FV-301", LocalDate.of(2026, 11, 26))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-170 C3 · El número de factura no se repite para el mismo proveedor (409)")
    void facturaRepetida() {
        recibirConRemision();
        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        when(cuentas.existsByNegocioIdAndProveedorIdAndNumeroFactura(negocioId, proveedor.getId(), "FV-100"))
                .thenReturn(true);

        assertThatThrownBy(() -> cuentasPorPagar.registrarFacturaDeCuenta(cuenta.getId(),
                new SolicitudDeFacturaDeCuenta("FV-100", LocalDate.of(2026, 11, 25))))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThat(cuenta.enEsperaDeFactura()).isTrue();
    }

    @Test
    @DisplayName("HU-170 C3 · La factura también se puede registrar por la recepción (POST de HU-049)")
    void porLaRecepcion() {
        recibirConRemision();

        CuentaDelNegocio vista = cuentasPorPagar.registrarFactura(
                new SolicitudDeFacturaDeRecepcion(recepcion.getId(), "FV-302", LocalDate.of(2026, 11, 22)));

        assertThat(vista.enEsperaDeFactura()).isFalse();
        assertThat(vista.fechaVencimiento()).isEqualTo(LocalDate.of(2026, 12, 22));
        assertThat(cuentasGuardadas).hasSize(1);
    }

    @Test
    @DisplayName("HU-170 C4 · Filtrar «en espera de factura» deja solo esas, con el número de su recepción")
    void filtroEnEspera() {
        recibirConRemision();
        CuentaPorPagar conFactura = CuentaPorPagar.abrirPorRecepcion(negocioId, proveedor.getId(), null, "FV-1",
                new BigDecimal("50000"), LocalDate.now(), 30);
        when(cuentas.findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId))
                .thenReturn(List.of(cuentasGuardadas.get(0), conFactura));

        List<CuentaDelNegocio> enEspera = cuentasPorPagar.listar(false, true);
        List<CuentaDelNegocio> todas = cuentasPorPagar.listar(false, false);

        assertThat(enEspera).hasSize(1);
        assertThat(enEspera.get(0).enEsperaDeFactura()).isTrue();
        assertThat(enEspera.get(0).recepcionNumero()).isEqualTo("REC-12");
        assertThat(enEspera.get(0).proveedorNombre()).isEqualTo("Distribuidora Andina SAS");
        assertThat(todas).hasSize(2);
    }
}
