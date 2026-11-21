package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infrastructure.ConsumidorDePagosAProveedor;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-145 · La caja registra sola la salida de un pago a proveedor en efectivo (I2, I3, I13 de Caja). */
class PagoAProveedorEnCajaTest {

    private final MovimientoDeCajaRepositorio movimientos = mock(MovimientoDeCajaRepositorio.class);
    private final SesionDeCajaRepositorio sesiones = mock(SesionDeCajaRepositorio.class);
    private final RegistroDeMovimientos registro = new RegistroDeMovimientos(movimientos, sesiones);

    private final UUID negocioId = UUID.randomUUID();
    private final List<MovimientoDeCaja> guardados = new ArrayList<>();
    private SesionDeCaja sesion;

    @BeforeEach
    void unaSesionAbierta() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, null, "", "VENTA_DIRECTA", Set.of(),
                Set.of("CAJA"), Set.of(), Set.of()));
        sesion = SesionDeCaja.abrir(negocioId, null, UUID.randomUUID(), "CAJA-5", UUID.randomUUID(),
                BigDecimal.ZERO);
        when(sesiones.findByIdAndNegocioId(sesion.getId(), negocioId)).thenReturn(Optional.of(sesion));
        when(movimientos.save(any())).thenAnswer(inv -> {
            guardados.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(movimientos.existsByNegocioIdAndIdempotencyKey(eq(negocioId), anyString()))
                .thenAnswer(inv -> guardados.stream()
                        .anyMatch(m -> m.getIdempotencyKey().equals(inv.getArgument(1))));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private Map<String, Object> pago(String metodo, UUID sesionCaja) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("pago_id", UUID.randomUUID().toString());
        datos.put("proveedor_nombre", "Andina");
        datos.put("numero_factura", "FV-100");
        datos.put("monto", new BigDecimal("100000"));
        datos.put("metodo", metodo);
        datos.put("sesion_caja_id", sesionCaja == null ? null : sesionCaja.toString());
        return datos;
    }

    @Test
    @DisplayName("HU-145 C1 · Efectivo desde la sesión: salida PAGO_PROVEEDOR por el mismo monto, con la factura, que baja el efectivo esperado")
    void salidaDeEfectivo() {
        assertThat(registro.registrarPagoAProveedor(pago("EFECTIVO", sesion.getId()))).isTrue();

        MovimientoDeCaja m = guardados.get(0);
        assertThat(m.getTipo()).isEqualTo(TipoMovimientoCaja.PAGO_PROVEEDOR);
        assertThat(m.getSigno()).isEqualTo((short) -1);
        assertThat(m.getMetodoPago()).isEqualTo(MetodoPagoCaja.EFECTIVO);
        assertThat(m.getMonto()).isEqualByComparingTo("100000");
        assertThat(m.getDocumentoRef()).isEqualTo("FV-100");
        assertThat(m.getConcepto()).isEqualTo("Pago a proveedor Andina");
        assertThat(m.aporteEfectivo()).isEqualByComparingTo("-100000");
    }

    @Test
    @DisplayName("HU-145 C2 · Por transferencia, cheque o tarjeta la caja no registra nada")
    void otroMetodoNoEntra() {
        for (String metodo : List.of("TRANSFERENCIA", "CHEQUE", "TARJETA")) {
            assertThat(registro.registrarPagoAProveedor(pago(metodo, sesion.getId()))).isFalse();
        }
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("HU-145 C3 · En efectivo pero sin sesión indicada, la caja no registra nada")
    void sinSesionNoEntra() {
        assertThat(registro.registrarPagoAProveedor(pago("EFECTIVO", null))).isFalse();
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("HU-145 C5 · El mismo pago dos veces no duplica la salida")
    void duplicadoNoDuplica() {
        Map<String, Object> mismo = pago("EFECTIVO", sesion.getId());

        registro.registrarPagoAProveedor(mismo);
        registro.registrarPagoAProveedor(mismo);

        assertThat(guardados).hasSize(1);
    }

    @Test
    @DisplayName("HU-145 C6 · La salida de un pago a proveedor no se anula desde la caja (422)")
    void noSeAnulaDesdeLaCaja() {
        registro.registrarPagoAProveedor(pago("EFECTIVO", sesion.getId()));

        assertThatThrownBy(() -> MovimientoDeCaja.anulacionDe(guardados.get(0), UUID.randomUUID(), "Error",
                "x:ANULACION"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-145 C7 · Una sesión de otro negocio no recibe el pago")
    void otroNegocio() {
        UUID sesionAjena = UUID.randomUUID();
        when(sesiones.findByIdAndNegocioId(sesionAjena, negocioId)).thenReturn(Optional.empty());

        assertThat(registro.registrarPagoAProveedor(pago("EFECTIVO", sesionAjena))).isFalse();
        verify(movimientos, never()).save(any());
    }

    @Test
    @DisplayName("HU-145 C5 · El consumidor pasa por el inbox y solo escucha pago_a_proveedor_registrado")
    @SuppressWarnings("unchecked")
    void consumidor() {
        InboxIdempotente inbox = mock(InboxIdempotente.class);
        doAnswer(i -> {
            ((Consumer<String>) i.getArgument(5)).accept(i.getArgument(4));
            return true;
        }).when(inbox).procesarUnaVez(anyString(), any(), any(), anyString(), anyString(), any());
        ConsumidorDePagosAProveedor consumidor = new ConsumidorDePagosAProveedor(inbox, registro, new ObjectMapper());

        consumidor.recibir(new EventoGenerico(UUID.randomUUID(), negocioId, "PagoProveedor", UUID.randomUUID(),
                "pago_a_proveedor_registrado", pago("EFECTIVO", sesion.getId()), null, OffsetDateTime.now()));
        assertThat(guardados).hasSize(1);
        verify(inbox).procesarUnaVez(eq("caja.ConsumidorDePagosAProveedor"), any(), eq(negocioId),
                eq("pago_a_proveedor_registrado"), anyString(), any());

        InboxIdempotente otro = mock(InboxIdempotente.class);
        new ConsumidorDePagosAProveedor(otro, registro, new ObjectMapper()).recibir(new EventoGenerico(
                UUID.randomUUID(), negocioId, "Venta", UUID.randomUUID(), "venta_pagada", Map.of(), null,
                OffsetDateTime.now()));
        verifyNoInteractions(otro);
    }
}
