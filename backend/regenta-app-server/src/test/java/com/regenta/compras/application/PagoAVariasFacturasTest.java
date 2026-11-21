package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.regenta.compras.domain.AplicacionDePago;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AplicacionDePagoRepositorio;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-171 · Un pago a varias facturas del mismo proveedor (I26, I29). */
class PagoAVariasFacturasTest {

    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final AplicacionDePagoRepositorio aplicaciones = mock(AplicacionDePagoRepositorio.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final GestionDeCuentasPorPagar gestion = new GestionDeCuentasPorPagar(cuentas, pagos,
            mock(RecepcionRepositorio.class), proveedores, caja, eventos, aplicaciones);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private Proveedor andina;
    private CuentaPorPagar vieja;
    private CuentaPorPagar nueva;
    private final List<AplicacionDePago> aplicadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        andina = proveedor("Distribuidora Andina SAS");
        vieja = CuentaPorPagar.abrirPorRecepcion(negocioId, andina.getId(), null, "FV-1", new BigDecimal("300000"),
                LocalDate.of(2026, 10, 1), 30);
        nueva = CuentaPorPagar.abrirPorRecepcion(negocioId, andina.getId(), null, "FV-2", new BigDecimal("500000"),
                LocalDate.of(2026, 11, 1), 30);
        when(cuentas.findByNegocioIdAndProveedorIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId, andina.getId()))
                .thenReturn(List.of(vieja, nueva));
        when(aplicaciones.saveAll(any())).thenAnswer(i -> {
            i.<Iterable<AplicacionDePago>>getArgument(0).forEach(aplicadas::add);
            return aplicadas;
        });
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.of(sesion));
        when(caja.sesionAbierta(negocioId, sesion)).thenReturn(true);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private Proveedor proveedor(String nombre) {
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, String.valueOf(nombre.hashCode()), nombre,
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        return p;
    }

    private SolicitudDePagoAProveedor pago(String monto, String metodo, Boolean desdeLaCaja,
            List<SolicitudDePagoAProveedor.Aplicacion> reparto) {
        return new SolicitudDePagoAProveedor(andina.getId(), new BigDecimal(monto), metodo, null, desdeLaCaja, reparto);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> eventoPublicado() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("PagoProveedor"), any(), eq("pago_a_proveedor_registrado"),
                payload.capture());
        return (Map<String, Object>) payload.getValue();
    }

    @Test
    @DisplayName("HU-171 C1 · $ 400.000 sin reparto: la de $ 300.000 (la más antigua) queda PAGADA y la otra PARCIAL con $ 400.000")
    void porAntiguedad() {
        PagoDelNegocio p = gestion.pagarAProveedor(pago("400000", "TRANSFERENCIA", null, null));

        assertThat(vieja.getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);
        assertThat(nueva.getEstado()).isEqualTo(EstadoCuentaPorPagar.PARCIAL);
        assertThat(nueva.getSaldo()).isEqualByComparingTo("400000");
        assertThat(p.aplicaciones()).extracting(PagoDelNegocio.Aplicada::numeroFactura).containsExactly("FV-1", "FV-2");
        assertThat(p.aplicaciones().get(0).monto()).isEqualByComparingTo("300000");
        assertThat(p.aplicaciones().get(1).monto()).isEqualByComparingTo("100000");
        assertThat(aplicadas).hasSize(2);
        ArgumentCaptor<PagoProveedor> guardado = ArgumentCaptor.forClass(PagoProveedor.class);
        verify(pagos).save(guardado.capture());
        assertThat(guardado.getValue().getMonto()).isEqualByComparingTo("400000");
        assertThat(guardado.getValue().getProveedorId()).isEqualTo(andina.getId());
    }

    @Test
    @DisplayName("HU-171 C2 · Con el reparto cambiado a $ 400.000 sobre la segunda, la primera no cambia y la segunda queda en $ 100.000")
    void comoSeIndico() {
        gestion.pagarAProveedor(pago("400000", "TRANSFERENCIA", null,
                List.of(new SolicitudDePagoAProveedor.Aplicacion(nueva.getId(), new BigDecimal("400000")))));

        assertThat(vieja.getSaldo()).isEqualByComparingTo("300000");
        assertThat(vieja.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(nueva.getSaldo()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("HU-171 C3 · $ 900.000 contra una deuda de $ 800.000 se rechaza (422): no se paga más de lo que se debe")
    void masDeLoQueSeDebe() {
        assertThatThrownBy(() -> gestion.pagarAProveedor(pago("900000", "TRANSFERENCIA", null, null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("No se paga más de lo que se debe");
        verify(pagos, never()).save(any());
        assertThat(vieja.getSaldo()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("HU-171 C4 · Una factura de otro proveedor en el reparto se rechaza (422)")
    void cuentaDeOtroProveedor() {
        Proveedor otro = proveedor("Pinturas del Valle");
        CuentaPorPagar ajena = CuentaPorPagar.abrirPorRecepcion(negocioId, otro.getId(), null, "PV-9",
                new BigDecimal("100000"), LocalDate.of(2026, 11, 1), 30);

        assertThatThrownBy(() -> gestion.pagarAProveedor(pago("100000", "TRANSFERENCIA", null,
                List.of(new SolicitudDePagoAProveedor.Aplicacion(ajena.getId(), new BigDecimal("100000"))))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no es de este proveedor");
        verify(pagos, never()).save(any());
    }

    @Test
    @DisplayName("HU-171 · Un reparto que no suma el monto del pago se rechaza (422)")
    void repartoQueNoSuma() {
        assertThatThrownBy(() -> gestion.pagarAProveedor(pago("400000", "TRANSFERENCIA", null, List.of(
                new SolicitudDePagoAProveedor.Aplicacion(vieja.getId(), new BigDecimal("100000"))))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("El reparto suma");
    }

    @Test
    @DisplayName("HU-171 · A una factura no se le abona más de su saldo (422)")
    void masQueElSaldo() {
        assertThatThrownBy(() -> gestion.pagarAProveedor(pago("400000", "TRANSFERENCIA", null, List.of(
                new SolicitudDePagoAProveedor.Aplicacion(vieja.getId(), new BigDecimal("400000"))))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("FV-1");
    }

    @Test
    @DisplayName("HU-171 C6 · En efectivo desde la caja sin una caja abierta: 409 y no se registra")
    void sinCajaAbierta() {
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestion.pagarAProveedor(pago("400000", "EFECTIVO", true, null)))
                .isInstanceOf(ConflictoDeEstadoException.class);
        verify(pagos, never()).save(any());
        assertThat(vieja.getSaldo()).isEqualByComparingTo("300000");
    }

    @Test
    @DisplayName("HU-171 / HU-145 · En efectivo desde la caja: una sola salida por el total, con la primera factura y cuántas más")
    void unaSalidaPorElTotal() {
        PagoDelNegocio p = gestion.pagarAProveedor(pago("400000", "EFECTIVO", true, null));

        Map<String, Object> e = eventoPublicado();
        assertThat((BigDecimal) e.get("monto")).isEqualByComparingTo("400000");
        assertThat(e.get("sesion_caja_id")).isEqualTo(sesion.toString());
        assertThat(e.get("numero_factura")).isEqualTo("FV-1 y 1 más");
        assertThat((List<?>) e.get("aplicaciones")).hasSize(2);
        assertThat(p.desdeLaCaja()).isTrue();
    }

    @Test
    @DisplayName("HU-171 · El detalle de una cuenta muestra lo que cada pago le abonó, no el pago entero")
    void detalleConLoAplicado() {
        gestion.pagarAProveedor(pago("400000", "TRANSFERENCIA", null, null));
        ArgumentCaptor<PagoProveedor> guardado = ArgumentCaptor.forClass(PagoProveedor.class);
        verify(pagos).save(guardado.capture());
        when(cuentas.findByIdAndNegocioId(nueva.getId(), negocioId)).thenReturn(Optional.of(nueva));
        when(aplicaciones.findByCuentaId(nueva.getId()))
                .thenReturn(aplicadas.stream().filter(a -> a.getCuentaId().equals(nueva.getId())).toList());
        when(pagos.findAllById(any())).thenReturn(List.of(guardado.getValue()));

        CuentaDelNegocio vista = gestion.ver(nueva.getId());

        assertThat(vista.pagos()).hasSize(1);
        assertThat(vista.pagos().get(0).monto()).isEqualByComparingTo("100000");
    }
}
