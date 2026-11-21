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
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

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
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.SinPermisoException;

/** HU-172 · Anular un pago a proveedor (I28, I29). */
class AnularPagoTest {

    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final AplicacionDePagoRepositorio aplicaciones = mock(AplicacionDePagoRepositorio.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sesionOriginal = UUID.randomUUID();
    private Proveedor andina;
    private CuentaPorPagar vieja;
    private CuentaPorPagar nueva;
    private final List<AplicacionDePago> aplicadas = new ArrayList<>();
    private final List<PagoProveedor> guardados = new ArrayList<>();

    private GestionDeCuentasPorPagar gestion() {
        return new GestionDeCuentasPorPagar(cuentas, pagos, mock(RecepcionRepositorio.class), proveedores, caja,
                eventos, aplicaciones);
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(permisos), Set.of()));
    }

    @BeforeEach
    void datos() {
        conPermisos("COMPRAS_COMPRA_CREAR", "COMPRAS_PAGO_ANULAR", "COMPRAS_COMPRA_VER");
        andina = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(andina.getId(), negocioId)).thenReturn(Optional.of(andina));
        vieja = CuentaPorPagar.abrirPorRecepcion(negocioId, andina.getId(), null, "FV-1", new BigDecimal("300000"),
                LocalDate.of(2026, 10, 1), 30);
        nueva = CuentaPorPagar.abrirPorRecepcion(negocioId, andina.getId(), null, "FV-2", new BigDecimal("500000"),
                LocalDate.of(2026, 11, 1), 30);
        for (CuentaPorPagar c : List.of(vieja, nueva)) {
            when(cuentas.findByIdAndNegocioId(c.getId(), negocioId)).thenReturn(Optional.of(c));
        }
        when(cuentas.findByNegocioIdAndProveedorIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId, andina.getId()))
                .thenReturn(List.of(vieja, nueva));
        when(aplicaciones.saveAll(any())).thenAnswer(i -> {
            i.<Iterable<AplicacionDePago>>getArgument(0).forEach(aplicadas::add);
            return aplicadas;
        });
        when(aplicaciones.findByPagoId(any())).thenAnswer(i -> aplicadas.stream()
                .filter(a -> a.getPagoId().equals(i.getArgument(0))).toList());
        when(pagos.save(any())).thenAnswer(i -> {
            PagoProveedor p = i.getArgument(0);
            if (!guardados.contains(p)) {
                guardados.add(p);
            }
            when(pagos.findById(p.getId())).thenReturn(Optional.of(p));
            return p;
        });
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.of(sesionOriginal));
        when(caja.sesionAbierta(negocioId, sesionOriginal)).thenReturn(true);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    /** El pago del criterio 1 de HU-171: $ 400.000 que dejan la vieja PAGADA y la nueva en $ 400.000. */
    private PagoDelNegocio pagar400(String metodo, Boolean desdeLaCaja) {
        return gestion().pagarAProveedor(new SolicitudDePagoAProveedor(andina.getId(), new BigDecimal("400000"),
                metodo, null, desdeLaCaja, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> eventoDeAnulacion() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("PagoProveedor"), any(), eq("pago_a_proveedor_anulado"),
                payload.capture());
        return (Map<String, Object>) payload.getValue();
    }

    @Test
    @DisplayName("HU-172 C1 · Anular el pago de HU-171 C1 devuelve las facturas a $ 300.000 y $ 500.000 y recalcula su estado")
    void devuelveLosSaldos() {
        PagoDelNegocio pago = pagar400("TRANSFERENCIA", null);
        assertThat(vieja.getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);

        gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Se registró dos veces", null));

        assertThat(vieja.getSaldo()).isEqualByComparingTo("300000");
        assertThat(nueva.getSaldo()).isEqualByComparingTo("500000");
        assertThat(vieja.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(nueva.getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(eventoDeAnulacion().get("sesion_reverso_id")).isNull();
    }

    @Test
    @DisplayName("HU-172 C2 · El pago anulado sigue visible con motivo, usuario y fecha; no se anula dos veces")
    void quedaVisible() {
        PagoDelNegocio pago = pagar400("TRANSFERENCIA", null);
        gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Se registró dos veces", null));

        PagoDelNegocio visto = gestion().verPago(pago.id());

        assertThat(visto.anulado()).isTrue();
        assertThat(visto.motivoAnulacion()).isEqualTo("Se registró dos veces");
        assertThat(visto.anuladoPor()).isEqualTo(usuarioId);
        assertThat(visto.anuladoEn()).isNotNull();
        assertThat(visto.aplicaciones()).hasSize(2);
        assertThatThrownBy(() -> gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Otra vez", null)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-172 C3 · Efectivo de una caja aún abierta: el reverso va a esa misma sesión")
    void reversoEnLaMismaCaja() {
        PagoDelNegocio pago = pagar400("EFECTIVO", true);

        gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Error de digitación", null));

        Map<String, Object> e = eventoDeAnulacion();
        assertThat(e.get("sesion_reverso_id")).isEqualTo(sesionOriginal.toString());
        assertThat((BigDecimal) e.get("monto")).isEqualByComparingTo("400000");
        assertThat(e.get("motivo")).isEqualTo("Error de digitación");
    }

    @Test
    @DisplayName("HU-172 C4 · Si esa caja ya cerró, el reverso entra a la sesión abierta que indique el usuario")
    void reversoEnOtraCaja() {
        PagoDelNegocio pago = pagar400("EFECTIVO", true);
        UUID otra = UUID.randomUUID();
        when(caja.sesionAbierta(negocioId, sesionOriginal)).thenReturn(false);
        when(caja.sesionAbierta(negocioId, otra)).thenReturn(true);

        gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Error", otra));

        assertThat(eventoDeAnulacion().get("sesion_reverso_id")).isEqualTo(otra.toString());
    }

    @Test
    @DisplayName("HU-172 C4 · Si esa caja ya cerró y no hay ninguna abierta, 409 y no se anula nada")
    void sinCajaParaElReverso() {
        PagoDelNegocio pago = pagar400("EFECTIVO", true);
        when(caja.sesionAbierta(negocioId, sesionOriginal)).thenReturn(false);
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Error", null)))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("ya cerró");
        assertThat(nueva.getSaldo()).isEqualByComparingTo("400000");
        assertThat(guardados.get(0).estaAnulado()).isFalse();
    }

    @Test
    @DisplayName("HU-172 C4 · Una sesión indicada que no está abierta se rechaza (409)")
    void sesionIndicadaCerrada() {
        PagoDelNegocio pago = pagar400("EFECTIVO", true);
        when(caja.sesionAbierta(negocioId, sesionOriginal)).thenReturn(false);
        UUID cerrada = UUID.randomUUID();
        when(caja.sesionAbierta(negocioId, cerrada)).thenReturn(false);

        assertThatThrownBy(() -> gestion().anularPago(pago.id(), new SolicitudDeAnulacionDePago("Error", cerrada)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-172 C5 · Sin COMPRAS_PAGO_ANULAR, anular responde 403")
    void sinPermiso() {
        PagoDelNegocio pago = pagar400("TRANSFERENCIA", null);
        conPermisos("COMPRAS_COMPRA_CREAR", "COMPRAS_COMPRA_VER");
        AspectJProxyFactory fabrica = new AspectJProxyFactory(gestion());
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        GestionDeCuentasPorPagar conGuardia = fabrica.getProxy();

        assertThatThrownBy(() -> conGuardia.anularPago(pago.id(), new SolicitudDeAnulacionDePago("Error", null)))
                .isInstanceOf(SinPermisoException.class);
        assertThat(nueva.getSaldo()).isEqualByComparingTo("400000");
        verify(eventos, never()).registrar(any(), any(), any(), eq("pago_a_proveedor_anulado"), any());
    }
}
