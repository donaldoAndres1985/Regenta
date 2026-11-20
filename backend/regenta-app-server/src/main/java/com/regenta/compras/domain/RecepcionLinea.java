package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un renglón de lo que llegó: qué producto, cuánto, a qué costo y —donde la
 * categoría lo exige— con qué lote y vencimiento (HU-048).
 *
 * <p>El lote se captura acá, no en la ficha del producto: el mismo medicamento
 * entra con lotes distintos cada semana. Si la categoría exige lote y no viene,
 * es 422 (criterio 1).
 */
@Entity
@Table(name = "recepcion_lineas", schema = "compras")
public class RecepcionLinea {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "recepcion_id", nullable = false, updatable = false)
    private UUID recepcionId;

    @Column(name = "orden_linea_id")
    private UUID ordenLineaId;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(name = "costo_unitario", nullable = false)
    private BigDecimal costoUnitario;

    @Column(name = "codigo_lote", length = 60)
    private String codigoLote;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "registro_sanitario", length = 60)
    private String registroSanitario;

    protected RecepcionLinea() {
    }

    /**
     * Una línea de lo recibido. {@code manejaLotes} y {@code perecedero} vienen del catálogo de
     * Inventario, nunca del cliente (HU-165, I16 de Compras): si el producto maneja lotes, el código
     * es obligatorio; si es perecedero, también el vencimiento; y un vencimiento anterior a la fecha
     * de recepción se rechaza.
     */
    public static RecepcionLinea nueva(UUID recepcionId, UUID negocioId, UUID ordenLineaId,
            UUID productoId, BigDecimal cantidad, BigDecimal costoUnitario, boolean manejaLotes,
            boolean perecedero, LocalDate fechaRecepcion, String codigoLote,
            LocalDate fechaVencimiento, String registroSanitario) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad recibida debe ser mayor que cero");
        }
        if (costoUnitario == null || costoUnitario.signum() < 0) {
            throw new ReglaDeNegocioException("El costo unitario no puede ser negativo");
        }
        String lote = limpiar(codigoLote);
        boolean exigeLote = manejaLotes || perecedero;
        if (exigeLote && lote == null) {
            throw new ReglaDeNegocioException(
                    "Este producto maneja lotes: indica el código de lote que llegó");
        }
        if (perecedero && fechaVencimiento == null) {
            throw new ReglaDeNegocioException(
                    "Este producto es perecedero: indica la fecha de vencimiento del lote");
        }
        if (exigeLote && fechaVencimiento != null && fechaVencimiento.isBefore(fechaRecepcion)) {
            throw new ReglaDeNegocioException("El lote " + lote + " venció el " + fechaVencimiento
                    + ": no se recibe mercancía vencida");
        }
        RecepcionLinea l = new RecepcionLinea();
        l.id = UUID.randomUUID();
        l.negocioId = negocioId;
        l.recepcionId = recepcionId;
        l.ordenLineaId = ordenLineaId;
        l.productoId = productoId;
        l.cantidad = cantidad;
        l.costoUnitario = costoUnitario;
        // Criterio 2: si no maneja lotes, no se guarda nada de lote aunque venga.
        l.codigoLote = exigeLote ? lote : null;
        l.fechaVencimiento = exigeLote ? fechaVencimiento : null;
        l.registroSanitario = exigeLote ? limpiar(registroSanitario) : null;
        return l;
    }

    public BigDecimal subtotal() {
        return cantidad.multiply(costoUnitario).setScale(4, RoundingMode.HALF_UP);
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

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getRecepcionId() {
        return recepcionId;
    }

    public UUID getOrdenLineaId() {
        return ordenLineaId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public String getRegistroSanitario() {
        return registroSanitario;
    }
}
