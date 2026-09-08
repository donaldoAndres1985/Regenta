package com.regenta.compras.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un producto bajo el mínimo esperando turno para una orden (HU-050). Nace de un
 * evento {@code stock_bajo_minimo}; al aceptar el grupo de un proveedor pasa a
 * {@code EN_ORDEN} con la orden creada.
 */
@Entity
@Table(name = "sugerencias_compra")
public class SugerenciaDeCompra {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "nombre_snapshot", nullable = false, length = 180)
    private String nombreSnapshot;

    @Column(name = "bodega_id")
    private UUID bodegaId;

    @Column(nullable = false)
    private BigDecimal existencia;

    @Column(name = "stock_minimo", nullable = false)
    private BigDecimal stockMinimo;

    @Column(name = "stock_objetivo", nullable = false)
    private BigDecimal stockObjetivo;

    @Column(name = "cantidad_sugerida", nullable = false)
    private BigDecimal cantidadSugerida;

    @Column(name = "proveedor_id")
    private UUID proveedorId;

    @Column(name = "costo_estimado")
    private BigDecimal costoEstimado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSugerencia estado;

    @Column(name = "orden_id")
    private UUID ordenId;

    @Column(name = "sin_proveedor", insertable = false, updatable = false)
    private boolean sinProveedor;

    @CreationTimestamp
    @Column(name = "creada_en", nullable = false, updatable = false)
    private OffsetDateTime creadaEn;

    @UpdateTimestamp
    @Column(name = "actualizada_en", nullable = false)
    private OffsetDateTime actualizadaEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected SugerenciaDeCompra() {
    }

    public static SugerenciaDeCompra desdeStockBajo(UUID negocioId, UUID productoId,
            String nombreSnapshot, UUID bodegaId, BigDecimal existencia, BigDecimal stockMinimo,
            BigDecimal stockObjetivo) {
        SugerenciaDeCompra s = new SugerenciaDeCompra();
        s.id = UUID.randomUUID();
        s.negocioId = negocioId;
        s.productoId = productoId;
        s.estado = EstadoSugerencia.PENDIENTE;
        s.refrescar(nombreSnapshot, bodegaId, existencia, stockMinimo, stockObjetivo);
        return s;
    }

    /** Vuelve a calcular la sugerencia con los últimos números del inventario. */
    public void refrescar(String nombreSnapshot, UUID bodegaId, BigDecimal existencia,
            BigDecimal stockMinimo, BigDecimal stockObjetivo) {
        this.nombreSnapshot = nombreSnapshot;
        this.bodegaId = bodegaId;
        this.existencia = noNegativo(existencia);
        this.stockMinimo = noNegativo(stockMinimo);
        BigDecimal objetivo = stockObjetivo != null && stockObjetivo.signum() > 0
                ? stockObjetivo
                : this.stockMinimo.max(BigDecimal.ONE);
        this.stockObjetivo = objetivo;
        recalcularCantidad(null);
    }

    /** Fija el proveedor preferido y el costo; ajusta la cantidad al mínimo del proveedor. */
    public void asignarProveedor(UUID proveedorId, BigDecimal costoEstimado,
            BigDecimal cantidadMinimaProveedor) {
        this.proveedorId = proveedorId;
        this.costoEstimado = costoEstimado;
        recalcularCantidad(cantidadMinimaProveedor);
    }

    private void recalcularCantidad(BigDecimal minimoProveedor) {
        BigDecimal faltante = stockObjetivo.subtract(existencia);
        if (faltante.signum() <= 0) {
            faltante = BigDecimal.ONE;
        }
        if (minimoProveedor != null && minimoProveedor.signum() > 0
                && faltante.compareTo(minimoProveedor) < 0) {
            faltante = minimoProveedor;
        }
        this.cantidadSugerida = faltante.setScale(6, RoundingMode.HALF_UP);
    }

    /** Criterio 3: al aceptarla queda enlazada a la orden que la surtió. */
    public void marcarEnOrden(UUID ordenId) {
        if (estado != EstadoSugerencia.PENDIENTE) {
            throw new ConflictoDeEstadoException("La sugerencia ya no está pendiente");
        }
        if (proveedorId == null) {
            throw new ReglaDeNegocioException(
                    "La sugerencia no tiene proveedor: asígnale uno antes de crear la orden");
        }
        this.estado = EstadoSugerencia.EN_ORDEN;
        this.ordenId = ordenId;
    }

    public void descartar() {
        if (estado == EstadoSugerencia.EN_ORDEN) {
            throw new ConflictoDeEstadoException("La sugerencia ya generó una orden");
        }
        this.estado = EstadoSugerencia.DESCARTADA;
    }

    private static BigDecimal noNegativo(BigDecimal v) {
        return v == null || v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getNombreSnapshot() {
        return nombreSnapshot;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public BigDecimal getExistencia() {
        return existencia;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public BigDecimal getStockObjetivo() {
        return stockObjetivo;
    }

    public BigDecimal getCantidadSugerida() {
        return cantidadSugerida;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public BigDecimal getCostoEstimado() {
        return costoEstimado;
    }

    public EstadoSugerencia getEstado() {
        return estado;
    }

    public UUID getOrdenId() {
        return ordenId;
    }

    public boolean isSinProveedor() {
        return proveedorId == null;
    }
}
