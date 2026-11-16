package com.regenta.caja.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-141 · la anulación de un movimiento manual (invariantes I5 e I6 de caja). */
class AnulacionDeMovimientoTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();
    private final UUID supervisor = UUID.randomUUID();

    private MovimientoDeCaja manual(TipoMovimientoCaja tipo, long monto) {
        return MovimientoDeCaja.manual(negocio, sesion, cajero, tipo, "Consignación",
                BigDecimal.valueOf(monto), null, "k-" + UUID.randomUUID());
    }

    @Test
    @DisplayName("HU-141 C1 · Dado un retiro, cuando se anula con motivo, el movimiento nuevo tiene signo contrario, mismo método y monto, apunta al original y el esperado vuelve a como estaba")
    void laAnulacionCompensaElOriginal() {
        MovimientoDeCaja retiro = manual(TipoMovimientoCaja.RETIRO, 40_000);

        MovimientoDeCaja anulacion = MovimientoDeCaja.anulacionDe(retiro, supervisor,
                "Se registró dos veces", "k-anul");

        assertThat(anulacion.getTipo()).isEqualTo(TipoMovimientoCaja.ANULACION);
        assertThat(anulacion.getSigno()).isEqualTo((short) -retiro.getSigno());
        assertThat(anulacion.getMetodoPago()).isEqualTo(retiro.getMetodoPago());
        assertThat(anulacion.getMonto()).isEqualByComparingTo(retiro.getMonto());
        assertThat(anulacion.getAnulaA()).isEqualTo(retiro.getId());
        assertThat(anulacion.getMotivoAnulacion()).isEqualTo("Se registró dos veces");
        assertThat(anulacion.getUsuarioId()).isEqualTo(supervisor);
        assertThat(retiro.aporteEfectivo().add(anulacion.aporteEfectivo())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("HU-141 C3 · Dado un motivo vacío, cuando intento anular, se rechaza con 422")
    void sinMotivoNoSeAnula() {
        MovimientoDeCaja ingreso = manual(TipoMovimientoCaja.INGRESO, 10_000);

        assertThatThrownBy(() -> MovimientoDeCaja.anulacionDe(ingreso, supervisor, "  ", "k"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    @DisplayName("HU-141 C4 · Dado un movimiento que no es manual (una venta), cuando intento anularlo, se rechaza con 422")
    void soloSeAnulanLosManuales() {
        MovimientoDeCaja venta = MovimientoDeCaja.deCobro(negocio, sesion, cajero,
                TipoMovimientoCaja.VENTA, "VENTA", UUID.randomUUID(), "FV-1", "Venta",
                MetodoPagoCaja.EFECTIVO, BigDecimal.valueOf(25_000), BigDecimal.ZERO, "k-venta");

        assertThatThrownBy(() -> MovimientoDeCaja.anulacionDe(venta, supervisor, "Error", "k"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("manual");
    }

    @Test
    @DisplayName("HU-141 C7 · Dada una anulación, el movimiento original queda intacto")
    void elOriginalNoCambia() {
        MovimientoDeCaja gasto = manual(TipoMovimientoCaja.GASTO, 8_000);
        BigDecimal monto = gasto.getMonto();
        short signo = gasto.getSigno();

        MovimientoDeCaja.anulacionDe(gasto, supervisor, "No era un gasto", "k");

        assertThat(gasto.getTipo()).isEqualTo(TipoMovimientoCaja.GASTO);
        assertThat(gasto.getMonto()).isEqualByComparingTo(monto);
        assertThat(gasto.getSigno()).isEqualTo(signo);
        assertThat(gasto.getAnulaA()).isNull();
    }
}
