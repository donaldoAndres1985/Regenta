package com.regenta.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.ventas.domain.MetodoDePago;
import com.regenta.ventas.domain.PagoDeVenta;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.infrastructure.AsignadorDeConsecutivos;
import com.regenta.ventas.infrastructure.PagoDeVentaRepositorio;
import com.regenta.ventas.infrastructure.VentaLineaRepositorio;
import com.regenta.ventas.infrastructure.VentaRepositorio;

/** HU-146 · las ventas del POS entran a la caja abierta del usuario que vende. */
class VentaEntraACajaTest {

    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final SagaDeConfirmacionDeVenta saga = mock(SagaDeConfirmacionDeVenta.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final PagoDeVentaRepositorio pagos = mock(PagoDeVentaRepositorio.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);

    private final GestionDeVentas gestion = new GestionDeVentas(ventas, mock(VentaLineaRepositorio.class),
            mock(AsignadorDeConsecutivos.class), saga, mock(ConfiguracionDelNegocio.class), caja, mock(PrecioDeLinea.class));
    private final GestionDePagos cobro = new GestionDePagos(ventas, pagos, eventos);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private Venta venta;

    @BeforeEach
    void unaVentaEnBorrador() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, cajero, "EMPRESARIAL",
                "VENTA_DIRECTA", Set.of(), Set.of(),
                Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_CONFIRMAR", "VENTAS_PAGO_REGISTRAR"), Set.of()));
        venta = Venta.crear(negocioId, UUID.randomUUID(), "41", null, cajero, null, "POS");
        when(ventas.findById(venta.getId())).thenReturn(Optional.of(venta));
        when(ventas.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-146 C1 · Dado un usuario con su caja abierta, cuando confirma una venta, la venta queda asociada a esa sesión")
    void laVentaQuedaEnLaSesionAbierta() {
        when(caja.sesionAbiertaDe(negocioId, cajero)).thenReturn(Optional.of(sesion));

        gestion.confirmar(venta.getId());

        assertThat(venta.getSesionCajaId()).isEqualTo(sesion);
        verify(saga).iniciar(eq(venta.getId()), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("HU-146 C2 · Dado un usuario sin caja abierta, cuando confirma una venta, se rechaza con 422 y no se reserva stock")
    void sinCajaAbiertaNoSeVende() {
        when(caja.sesionAbiertaDe(negocioId, cajero)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gestion.confirmar(venta.getId()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Abre la caja antes de vender.");
        verify(saga, never()).iniciar(any(), any(java.time.Duration.class));
        assertThat(venta.getSesionCajaId()).isNull();
    }

    @Test
    @DisplayName("HU-146 C3 · Dada una venta cobrada sin señal que se sube sin caja abierta, no se rechaza y queda sin sesión")
    void laVentaOfflineNoSeRechaza() {
        when(caja.sesionAbiertaDe(negocioId, cajero)).thenReturn(Optional.empty());

        gestion.confirmarLaQueYaSeCobroSinSenal(venta.getId());

        assertThat(venta.getSesionCajaId()).isNull();
        verify(saga).iniciar(eq(venta.getId()), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("HU-146 C4 · Dado el cierre del cobro, venta_pagada lleva la sesión, el número, quién cobró y cada pago con su método y monto")
    @SuppressWarnings("unchecked")
    void ventaPagadaLlevaSesionYPagos() {
        venta.asociarASesionDeCaja(sesion);
        venta.aPendienteStock();
        venta.aConfirmada();
        when(pagos.sumaAplicadaDe(venta.getId())).thenReturn(venta.getTotal());
        when(pagos.findByVentaIdOrderByRecibidoEn(venta.getId())).thenReturn(List.of(
                PagoDeVenta.de(negocioId, venta.getId(), MetodoDePago.EFECTIVO, new BigDecimal("30000"),
                        new BigDecimal("50000"), new BigDecimal("20000"), null, null),
                PagoDeVenta.de(negocioId, venta.getId(), MetodoDePago.TARJETA_DEBITO,
                        new BigDecimal("16000"), null, BigDecimal.ZERO, "123", "VISA")));

        cobro.cerrarVenta(venta.getId());

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("Venta"), eq(venta.getId()), eq("venta_pagada"),
                payload.capture());
        Map<String, Object> datos = (Map<String, Object>) payload.getValue();
        assertThat(datos).containsEntry("sesion_caja_id", sesion.toString())
                .containsEntry("numero", "41")
                .containsEntry("usuario_id", cajero.toString());
        List<Map<String, Object>> enviados = (List<Map<String, Object>>) datos.get("pagos");
        assertThat(enviados).extracting(p -> p.get("metodo"))
                .containsExactly("EFECTIVO", "TARJETA_DEBITO");
        assertThat((BigDecimal) enviados.get(0).get("monto")).isEqualByComparingTo("30000");
    }
}
