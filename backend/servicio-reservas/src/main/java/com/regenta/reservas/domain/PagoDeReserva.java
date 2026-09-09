package com.regenta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** Un movimiento de dinero sobre una reserva (HU-071): anticipo, saldo, penalización… */
public final class PagoDeReserva {

    private final UUID id;
    private final UUID negocioId;
    private final UUID reservaId;
    private final TipoDePagoReserva tipo;
    private final MetodoDePago metodo;
    private final BigDecimal monto;
    private final String referencia;
    private final UUID cajaSesionId;
    private final UUID usuarioId;

    private PagoDeReserva(UUID id, UUID negocioId, UUID reservaId, TipoDePagoReserva tipo,
            MetodoDePago metodo, BigDecimal monto, String referencia, UUID cajaSesionId,
            UUID usuarioId) {
        this.id = id;
        this.negocioId = negocioId;
        this.reservaId = reservaId;
        this.tipo = tipo;
        this.metodo = metodo;
        this.monto = monto;
        this.referencia = referencia;
        this.cajaSesionId = cajaSesionId;
        this.usuarioId = usuarioId;
    }

    public static PagoDeReserva nuevo(UUID negocioId, UUID reservaId, TipoDePagoReserva tipo,
            MetodoDePago metodo, BigDecimal monto, String referencia, UUID cajaSesionId,
            UUID usuarioId) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor que cero");
        }
        return new PagoDeReserva(UUID.randomUUID(), negocioId, reservaId, tipo,
                metodo == null ? MetodoDePago.OTRO : metodo, monto.setScale(4, RoundingMode.HALF_UP),
                referencia == null || referencia.isBlank() ? null : referencia.trim(), cajaSesionId,
                usuarioId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public TipoDePagoReserva getTipo() {
        return tipo;
    }

    public MetodoDePago getMetodo() {
        return metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public UUID getCajaSesionId() {
        return cajaSesionId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }
}
