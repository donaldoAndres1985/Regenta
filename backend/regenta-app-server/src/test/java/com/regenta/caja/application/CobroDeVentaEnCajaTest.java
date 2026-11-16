package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import com.regenta.caja.infrastructure.ConsumidorDeCobros;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.events.EventoGenerico;
import com.regenta.shared.events.InboxIdempotente;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-146 · la caja registra los pagos de venta_pagada en la sesión de la venta. */
class CobroDeVentaEnCajaTest {

    private final MovimientoDeCajaRepositorio movimientos = mock(MovimientoDeCajaRepositorio.class);
    private final SesionDeCajaRepositorio sesiones = mock(SesionDeCajaRepositorio.class);
    private final InboxIdempotente inbox = mock(InboxIdempotente.class);
    private final RegistroDeMovimientos registro = new RegistroDeMovimientos(movimientos, sesiones);
    private final ConsumidorDeCobros consumidor =
            new ConsumidorDeCobros(inbox, registro, new ObjectMapper());

    private final UUID negocioId = UUID.randomUUID();
    private final List<MovimientoDeCaja> guardados = new ArrayList<>();
    private SesionDeCaja sesion;

    @BeforeEach
    void unaSesionAbierta() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "EMPRESARIAL",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(), Set.of()));
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

    private Map<String, Object> ventaPagada(UUID ventaId) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("venta_id", ventaId.toString());
        datos.put("numero", "41");
        datos.put("sesion_caja_id", sesion.getId().toString());
        datos.put("pagos", List.of(
                Map.of("metodo", "EFECTIVO", "monto", new BigDecimal("30000")),
                Map.of("metodo", "TARJETA_DEBITO", "monto", new BigDecimal("16000"))));
        return datos;
    }

    @Test
    @DisplayName("HU-146 C5 · Dado venta_pagada de una sesión abierta, la caja registra un movimiento VENTA por cada pago con su método")
    @SuppressWarnings("unchecked")
    void cadaPagoEntraALaCaja() {
        doAnswer(inv -> {
            ((Consumer<String>) inv.getArgument(5)).accept(inv.getArgument(4));
            return true;
        }).when(inbox).procesarUnaVez(anyString(), any(), any(), anyString(), anyString(), any());
        UUID ventaId = UUID.randomUUID();

        consumidor.recibir(new EventoGenerico(negocioId, "Venta", ventaId, "venta_pagada",
                ventaPagada(ventaId), null));

        assertThat(guardados).hasSize(2);
        assertThat(guardados).allSatisfy(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimientoCaja.VENTA);
            assertThat(m.getSesionId()).isEqualTo(sesion.getId());
            assertThat(m.getDocumentoRef()).isEqualTo("41");
        });
        assertThat(guardados).extracting(MovimientoDeCaja::getMetodoPago)
                .containsExactly(MetodoPagoCaja.EFECTIVO, MetodoPagoCaja.TARJETA_DEBITO);
        assertThat(guardados.get(0).aporteEfectivo()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("HU-146 C5 · venta_completada ya no mueve la caja: sale antes de que existan los pagos")
    void ventaCompletadaNoMueveLaCaja() {
        UUID ventaId = UUID.randomUUID();

        consumidor.recibir(new EventoGenerico(negocioId, "Venta", ventaId, "venta_completada",
                ventaPagada(ventaId), null));

        verify(inbox, never()).procesarUnaVez(anyString(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("HU-146 C6 · Dado el mismo venta_pagada dos veces, la caja no registra los pagos otra vez")
    void elDuplicadoNoSeRegistra() {
        UUID ventaId = UUID.randomUUID();

        int primera = registro.registrarDesdeCobro("VENTA", ventaPagada(ventaId));
        int segunda = registro.registrarDesdeCobro("VENTA", ventaPagada(ventaId));

        assertThat(primera).isEqualTo(2);
        assertThat(segunda).isZero();
        assertThat(guardados).hasSize(2);
    }
}
