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
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;

/** HU-145 · Pago a proveedor en efectivo desde la caja: el lado de Compras (I29). */
class PagoDesdeLaCajaTest {

    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final GestionDeCuentasPorPagar gestion = new GestionDeCuentasPorPagar(cuentas, pagos,
            mock(RecepcionRepositorio.class), proveedores, caja, eventos);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private CuentaPorPagar cuenta;

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", "Andina", null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        cuenta = CuentaPorPagar.abrirPorRecepcion(negocioId, p.getId(), null, "FV-100",
                new BigDecimal("248000"), LocalDate.of(2026, 11, 20), 30);
        when(cuentas.findByIdAndNegocioId(cuenta.getId(), negocioId)).thenReturn(Optional.of(cuenta));
        when(pagos.findByCuentaIdOrderByFechaAsc(cuenta.getId())).thenReturn(List.of());
        when(caja.sesionAbierta(negocioId, sesion)).thenReturn(true);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private SolicitudDePago pago(String monto, String metodo, UUID sesionCaja) {
        return new SolicitudDePago(new BigDecimal(monto), metodo, null, sesionCaja);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> eventoPublicado() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("PagoProveedor"), any(), eq("pago_a_proveedor_registrado"),
                payload.capture());
        return (Map<String, Object>) payload.getValue();
    }

    private PagoProveedor pagoGuardado() {
        ArgumentCaptor<PagoProveedor> p = ArgumentCaptor.forClass(PagoProveedor.class);
        verify(pagos).save(p.capture());
        return p.getValue();
    }

    @Test
    @DisplayName("HU-145 C1 · Efectivo desde una caja abierta: el evento lleva la sesión, el monto, el proveedor y la factura")
    void efectivoDesdeLaCaja() {
        gestion.registrarPago(cuenta.getId(), pago("100000", "EFECTIVO", sesion));

        Map<String, Object> e = eventoPublicado();
        assertThat(e.get("sesion_caja_id")).isEqualTo(sesion.toString());
        assertThat(e.get("metodo")).isEqualTo("EFECTIVO");
        assertThat((BigDecimal) e.get("monto")).isEqualByComparingTo("100000");
        assertThat(e.get("numero_factura")).isEqualTo("FV-100");
        assertThat(e.get("proveedor_nombre")).isEqualTo("Andina");
        assertThat(pagoGuardado().getSesionCajaId()).isEqualTo(sesion);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("148000");
    }

    @Test
    @DisplayName("HU-145 C2 · Por transferencia no se toca la caja, aunque venga una sesión")
    void transferenciaNoTocaLaCaja() {
        gestion.registrarPago(cuenta.getId(), pago("100000", "TRANSFERENCIA", sesion));

        assertThat(eventoPublicado().get("sesion_caja_id")).isNull();
        assertThat(pagoGuardado().salioDeLaCaja()).isFalse();
        verify(caja, never()).sesionAbierta(any(), any());
    }

    @Test
    @DisplayName("HU-145 C3 · Efectivo sin sesión indicada: se pagó con dinero de fuera de la caja")
    void efectivoSinSesion() {
        gestion.registrarPago(cuenta.getId(), pago("100000", "EFECTIVO", null));

        assertThat(eventoPublicado().get("sesion_caja_id")).isNull();
        assertThat(pagoGuardado().salioDeLaCaja()).isFalse();
    }

    @Test
    @DisplayName("HU-145 C4 · Efectivo con una sesión que no está abierta o no existe: 409 y no se registra el pago")
    void sesionNoAbierta() {
        UUID cerrada = UUID.randomUUID();
        when(caja.sesionAbierta(negocioId, cerrada)).thenReturn(false);

        assertThatThrownBy(() -> gestion.registrarPago(cuenta.getId(), pago("100000", "EFECTIVO", cerrada)))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("no está abierta");
        verify(pagos, never()).save(any());
        verify(eventos, never()).registrar(any(), any(), any(), any(), any());
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("248000");
    }
}
