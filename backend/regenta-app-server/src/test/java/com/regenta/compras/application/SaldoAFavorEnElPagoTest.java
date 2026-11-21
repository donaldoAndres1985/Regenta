package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.AplicacionDePago;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.NotaCreditoProveedor;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AplicacionDePagoRepositorio;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.NotaCreditoRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/**
 * HU-171 C5 (issue #332) y HU-178 · El saldo a favor que dejan las notas crédito se ofrece al pagarle al
 * proveedor: lo aplicado suma el monto más el saldo a favor usado. Anular el pago lo devuelve, y la
 * deuda con el proveedor es saldos abiertos menos saldo a favor (I5).
 */
class SaldoAFavorEnElPagoTest {

    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final AplicacionDePagoRepositorio aplicaciones = mock(AplicacionDePagoRepositorio.class);
    private final NotaCreditoRepositorio notas = mock(NotaCreditoRepositorio.class);
    private final GestionDeCuentasPorPagar gestion = new GestionDeCuentasPorPagar(cuentas, pagos,
            mock(RecepcionRepositorio.class), proveedores, mock(CajaApi.class), mock(RegistroDeEventos.class),
            aplicaciones, notas);

    private final UUID negocioId = UUID.randomUUID();
    private Proveedor andina;
    private CuentaPorPagar vieja;
    private CuentaPorPagar nueva;
    private NotaCreditoProveedor nota;
    private final List<AplicacionDePago> aplicadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        andina = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS", null,
                null, null, null, null, null, 30, new BigDecimal("700000"), null, null);
        when(proveedores.findByIdAndNegocioId(andina.getId(), negocioId)).thenReturn(Optional.of(andina));
        vieja = CuentaPorPagar.abrirPorRecepcion(negocioId, andina.getId(), null, "FV-1", new BigDecimal("300000"),
                LocalDate.of(2026, 10, 1), 30);
        nueva = CuentaPorPagar.abrirPorRecepcion(negocioId, andina.getId(), null, "FV-2", new BigDecimal("500000"),
                LocalDate.of(2026, 11, 1), 30);
        when(cuentas.findByNegocioIdAndProveedorIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId, andina.getId()))
                .thenReturn(List.of(vieja, nueva));
        when(cuentas.findByIdAndNegocioId(vieja.getId(), negocioId)).thenReturn(Optional.of(vieja));
        when(cuentas.findByIdAndNegocioId(nueva.getId(), negocioId)).thenReturn(Optional.of(nueva));
        when(aplicaciones.saveAll(any())).thenAnswer(i -> {
            i.<Iterable<AplicacionDePago>>getArgument(0).forEach(aplicadas::add);
            return aplicadas;
        });

        // Una devolución de una factura ya pagada dejó $ 50.000 a favor.
        Recepcion pagada = Recepcion.crear(negocioId, "REC-1", null, andina.getId(), UUID.randomUUID(), "FV-0", null,
                null);
        DevolucionAProveedor devolucion = DevolucionAProveedor.registrar(negocioId, "DEV-1", pagada, "Averiado",
                new BigDecimal("50000"), null);
        nota = NotaCreditoProveedor.emitir(negocioId, devolucion, null, new BigDecimal("50000"));
        when(notas.saldoAFavorDe(negocioId, andina.getId())).thenAnswer(i -> nota.getSaldoAFavor());
        when(notas.findByNegocioIdAndProveedorIdOrderByCreadoEnAsc(negocioId, andina.getId())).thenReturn(List.of(nota));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private PagoDelNegocio pagar(String monto, Boolean usar) {
        return gestion.pagarAProveedor(new SolicitudDePagoAProveedor(andina.getId(), new BigDecimal(monto),
                "TRANSFERENCIA", null, null, null, usar));
    }

    @Test
    @DisplayName("HU-171 C5 · Pagar $ 400.000 usando $ 50.000 a favor abona $ 450.000: la primera PAGADA, la segunda con $ 350.000")
    void usaElSaldoAFavor() {
        PagoDelNegocio pago = pagar("400000", true);

        assertThat(pago.monto()).isEqualByComparingTo("400000");
        assertThat(pago.saldoAFavorUsado()).isEqualByComparingTo("50000");
        assertThat(pago.aplicaciones().stream().map(PagoDelNegocio.Aplicada::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("450000");
        assertThat(vieja.getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
        assertThat(nueva.getSaldo()).isEqualByComparingTo("350000");
        assertThat(nota.getSaldoAFavor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Sin pedirlo, el saldo a favor no se toca")
    void sinUsarlo() {
        PagoDelNegocio pago = pagar("400000", null);

        assertThat(pago.saldoAFavorUsado()).isEqualByComparingTo("0");
        assertThat(nueva.getSaldo()).isEqualByComparingTo("400000");
        assertThat(nota.getSaldoAFavor()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("El saldo a favor no paga más de lo que se debe: con $ 780.000 de pago solo se usan $ 20.000")
    void noPasaLaDeuda() {
        PagoDelNegocio pago = pagar("780000", true);

        assertThat(pago.saldoAFavorUsado()).isEqualByComparingTo("20000");
        assertThat(nueva.getSaldo()).isEqualByComparingTo("0");
        assertThat(nota.getSaldoAFavor()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("HU-172 · Anular el pago devuelve el saldo a favor que usó")
    void anularLoDevuelve() {
        PagoDelNegocio pago = pagar("400000", true);
        ArgumentCaptor<PagoProveedor> guardado = ArgumentCaptor.forClass(PagoProveedor.class);
        Mockito.verify(pagos).save(guardado.capture());
        when(pagos.findById(pago.id())).thenReturn(Optional.of(guardado.getValue()));
        when(aplicaciones.findByPagoId(pago.id())).thenReturn(List.copyOf(aplicadas));

        gestion.anularPago(pago.id(), new SolicitudDeAnulacionDePago("Se pagó dos veces", null));

        assertThat(nota.getSaldoAFavor()).isEqualByComparingTo("50000");
        assertThat(vieja.getSaldo()).isEqualByComparingTo("300000");
        assertThat(nueva.getSaldo()).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("I5 · La deuda del proveedor es $ 800.000 abiertos menos $ 50.000 a favor")
    void deudaNeta() {
        when(cuentas.saldosAbiertosPorProveedor(negocioId))
                .thenReturn(List.<Object[]>of(new Object[] {andina.getId(), new BigDecimal("800000")}));
        when(notas.saldosAFavorPorProveedor(negocioId))
                .thenReturn(List.<Object[]>of(new Object[] {andina.getId(), new BigDecimal("50000")}));
        when(proveedores.findByNegocioIdOrderByRazonSocialAsc(negocioId)).thenReturn(List.of(andina));
        GestionDeProveedores deProveedores = new GestionDeProveedores(proveedores,
                mock(ProveedorProductoRepositorio.class), cuentas, mock(InventarioApi.class), notas);

        ProveedorDelNegocio visto = deProveedores.listar(null).get(0);

        assertThat(visto.deuda()).isEqualByComparingTo("750000");
        assertThat(visto.saldoAFavor()).isEqualByComparingTo("50000");
        assertThat(visto.superaCupo()).isTrue();
        Mockito.verify(notas).saldosAFavorPorProveedor(eq(negocioId));
    }
}
