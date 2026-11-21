package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un pago hecho a un proveedor (HU-049, HU-145, HU-171). Puede abonar a varias de sus cuentas: lo que
 * abonó a cada una está en {@link AplicacionDePago}. {@code cuentaId} solo se llena cuando abonó a una
 * sola cuenta.
 */
@Entity
@Table(name = "pagos_proveedor", schema = "compras")
public class PagoProveedor {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "proveedor_id", nullable = false, updatable = false)
    private UUID proveedorId;

    @Column(name = "cuenta_id", updatable = false)
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

    /** HU-172 · Cuándo, quién y por qué se anuló; nulos mientras el pago vale. */
    @Column(name = "anulado_en")
    private OffsetDateTime anuladoEn;

    @Column(name = "anulado_por")
    private UUID anuladoPor;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;

    /** HU-172 · La sesión de caja que recibe el reverso del efectivo, si el pago salió de la caja. */
    @Column(name = "sesion_reverso_id")
    private UUID sesionReversoId;

    /** HU-145 · La sesión de caja de la que salió el efectivo; nula si se pagó por fuera. */
    @Column(name = "sesion_caja_id", updatable = false)
    private UUID sesionCajaId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected PagoProveedor() {
    }

    /**
     * HU-145 · Solo un pago en efectivo puede salir de una sesión de caja; con otro método la sesión
     * no se guarda (la caja no cambia). {@code cuentaId} es nulo si el pago abona a varias cuentas.
     */
    public static PagoProveedor registrar(UUID negocioId, UUID proveedorId, UUID cuentaId,
            BigDecimal monto, MetodoDePago metodo, String referencia, UUID usuarioId,
            UUID sesionCajaId) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El pago debe ser mayor que cero");
        }
        PagoProveedor p = new PagoProveedor();
        p.id = UUID.randomUUID();
        p.negocioId = negocioId;
        p.proveedorId = proveedorId;
        p.cuentaId = cuentaId;
        p.monto = monto;
        p.metodo = metodo == null ? MetodoDePago.OTRO : metodo;
        p.referencia = limpiar(referencia);
        p.fecha = LocalDate.now();
        p.usuarioId = usuarioId;
        p.sesionCajaId = p.metodo == MetodoDePago.EFECTIVO ? sesionCajaId : null;
        return p;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    /**
     * HU-172 · I28: un pago no se edita ni se borra; se anula con motivo. Anular dos veces es un
     * conflicto (409).
     */
    public void anular(UUID usuarioId, String motivo, UUID sesionReverso) {
        if (estaAnulado()) {
            throw new com.regenta.shared.web.ConflictoDeEstadoException("Ese pago ya está anulado");
        }
        String limpio = limpiar(motivo);
        if (limpio == null) {
            throw new ReglaDeNegocioException("La anulación de un pago exige un motivo");
        }
        this.anuladoEn = OffsetDateTime.now();
        this.anuladoPor = usuarioId;
        this.motivoAnulacion = limpio.length() > 200 ? limpio.substring(0, 200) : limpio;
        this.sesionReversoId = salioDeLaCaja() ? sesionReverso : null;
    }

    public boolean estaAnulado() {
        return anuladoEn != null;
    }

    public OffsetDateTime getAnuladoEn() {
        return anuladoEn;
    }

    public UUID getAnuladoPor() {
        return anuladoPor;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public UUID getSesionReversoId() {
        return sesionReversoId;
    }

    public boolean salioDeLaCaja() {
        return sesionCajaId != null;
    }

    public UUID getSesionCajaId() {
        return sesionCajaId;
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
