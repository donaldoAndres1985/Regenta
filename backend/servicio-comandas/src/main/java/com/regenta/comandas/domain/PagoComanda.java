package com.regenta.comandas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Lo que la caja registra al cobrar una cuenta (HU-090 criterio 5): método,
 * cuánto se recibió y el cambio si fue en efectivo, y la propina aparte del
 * total — en Colombia es voluntaria y no forma parte de la base gravable.
 */
@Entity
@Table(name = "pagos_comanda")
public class PagoComanda {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "comanda_id", nullable = false, updatable = false)
    private UUID comandaId;

    @Column(name = "cuenta_id", updatable = false)
    private UUID cuentaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoDePago metodo;

    @Column(nullable = false)
    private BigDecimal monto;

    @Column(nullable = false)
    private BigDecimal propina;

    @Column(name = "monto_recibido")
    private BigDecimal montoRecibido;

    @Column(nullable = false)
    private BigDecimal cambio;

    @Column(length = 80)
    private String referencia;

    @Column(name = "caja_sesion_id")
    private UUID cajaSesionId;

    @Column(name = "recibido_en", nullable = false, updatable = false)
    private OffsetDateTime recibidoEn;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    protected PagoComanda() {
    }

    public static PagoComanda registrar(UUID negocioId, UUID comandaId, UUID cuentaId, MetodoDePago metodo,
            BigDecimal monto, BigDecimal propina, BigDecimal montoRecibido, String referencia, UUID usuarioId) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor que cero");
        }
        PagoComanda p = new PagoComanda();
        p.id = UUID.randomUUID();
        p.negocioId = negocioId;
        p.comandaId = comandaId;
        p.cuentaId = cuentaId;
        p.metodo = metodo;
        p.monto = escala4(monto);
        p.propina = escala4(propina);
        p.montoRecibido = montoRecibido == null ? null : escala4(montoRecibido);
        p.cambio = p.montoRecibido != null && p.montoRecibido.compareTo(p.monto) > 0
                ? escala4(p.montoRecibido.subtract(p.monto))
                : BigDecimal.ZERO;
        p.referencia = referencia == null || referencia.isBlank() ? null : referencia.trim();
        p.recibidoEn = OffsetDateTime.now();
        p.usuarioId = usuarioId;
        return p;
    }

    private static BigDecimal escala4(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(4, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getComandaId() {
        return comandaId;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public MetodoDePago getMetodo() {
        return metodo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public BigDecimal getPropina() {
        return propina;
    }

    public BigDecimal getMontoRecibido() {
        return montoRecibido;
    }

    public BigDecimal getCambio() {
        return cambio;
    }

    public String getReferencia() {
        return referencia;
    }

    public UUID getCajaSesionId() {
        return cajaSesionId;
    }

    public OffsetDateTime getRecibidoEn() {
        return recibidoEn;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }
}
