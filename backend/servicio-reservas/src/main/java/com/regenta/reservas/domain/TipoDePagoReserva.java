package com.regenta.reservas.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Qué representa un movimiento de {@code pagos_reserva}. Coincide con el CHECK de
 * la tabla. {@link #abonaAlSaldo()} distingue lo que cuenta como pago del
 * huésped hacia la reserva.
 */
public enum TipoDePagoReserva {

    ANTICIPO(true),
    SALDO(true),
    DEPOSITO(true),
    PENALIZACION(false),
    REEMBOLSO(false),
    CONSUMO(true);

    private final boolean abona;

    TipoDePagoReserva(boolean abona) {
        this.abona = abona;
    }

    public boolean abonaAlSaldo() {
        return abona;
    }

    public static TipoDePagoReserva desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ReglaDeNegocioException("El pago necesita un tipo");
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Tipo de pago no válido: " + texto);
        }
    }
}
