package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un pago hecho a un proveedor contra una cuenta por pagar (HU-049). */
@Entity
@Table(name = "pagos_proveedor")
public class PagoProveedor {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(nullable = false, updatable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private MetodoDePago metodo;

    @Column(length = 60, updatable = false)
    private String referencia;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected PagoProveedor() {
    }

    public static PagoProveedor registrar(UUID negocioId, UUID cuentaId, BigDecimal monto,
            MetodoDePago metodo, String referencia, UUID usuarioId) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El pago debe ser mayor que cero");
        }
        PagoProveedor p = new PagoProveedor();
        p.id = UUID.randomUUID();
        p.negocioId = negocioId;
        p.cuentaId = cuentaId;
        p.monto = monto;
        p.metodo = metodo == null ? MetodoDePago.OTRO : metodo;
        p.referencia = limpiar(referencia);
        p.fecha = LocalDate.now();
        p.usuarioId = usuarioId;
        return p;
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MetodoDePago getMetodo() {
        return metodo;
    }

    public String getReferencia() {
        return referencia;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
