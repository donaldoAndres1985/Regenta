package com.regenta.comandas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-090. Reglas puras del pago: el cambio en efectivo, y que el método no falte. */
class PagoComandaTest {

    @Test
    @DisplayName("Criterio 5: en efectivo, el cambio es lo que sobra del monto recibido")
    void calculaElCambio() {
        PagoComanda p = PagoComanda.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                MetodoDePago.EFECTIVO, new BigDecimal("47000"), BigDecimal.ZERO, new BigDecimal("50000"), null,
                UUID.randomUUID());

        assertThat(p.getCambio()).isEqualByComparingTo("3000");
    }

    @Test
    @DisplayName("Sin monto recibido, o pagando exacto, no hay cambio")
    void sinCambio() {
        PagoComanda exacto = PagoComanda.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                MetodoDePago.TARJETA_DEBITO, new BigDecimal("47000"), BigDecimal.ZERO, new BigDecimal("47000"),
                "voucher 004182", UUID.randomUUID());
        assertThat(exacto.getCambio()).isEqualByComparingTo("0");
        assertThat(exacto.getReferencia()).isEqualTo("voucher 004182");

        PagoComanda sinRecibido = PagoComanda.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                MetodoDePago.TRANSFERENCIA, new BigDecimal("47000"), BigDecimal.ZERO, null, null, UUID.randomUUID());
        assertThat(sinRecibido.getCambio()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Un monto no positivo se rechaza")
    void montoInvalido() {
        assertThatThrownBy(() -> PagoComanda.registrar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                MetodoDePago.EFECTIVO, BigDecimal.ZERO, BigDecimal.ZERO, null, null, UUID.randomUUID()))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("MetodoDePago.desde exige un método real: no cae a un valor por defecto")
    void metodoObligatorio() {
        assertThatThrownBy(() -> MetodoDePago.desde(null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> MetodoDePago.desde("")).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> MetodoDePago.desde("bitcoin")).isInstanceOf(ReglaDeNegocioException.class);
        assertThat(MetodoDePago.desde("tarjeta_credito")).isEqualTo(MetodoDePago.TARJETA_CREDITO);
    }
}
