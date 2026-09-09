package com.regenta.menu.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una línea de la receta de un ítem (HU-079): un producto de inventario con la
 * cantidad que consume y su merma. El par {@code (item_menu_id, producto_id)} es
 * único ({@code uq_receta}).
 */
@Entity
@Table(name = "recetas")
public class Receta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "item_menu_id", nullable = false, updatable = false)
    private UUID itemMenuId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private UUID productoId;

    @Column(name = "nombre_snapshot", length = 180)
    private String nombreSnapshot;

    @Column(nullable = false)
    private BigDecimal cantidad;

    @Column(length = 20)
    private String unidad;

    @Column(name = "merma_pct", nullable = false)
    private BigDecimal mermaPct;

    @Column(nullable = false)
    private boolean opcional;

    protected Receta() {
    }

    public static Receta crear(UUID negocioId, UUID itemMenuId, UUID productoId,
            String nombreSnapshot, BigDecimal cantidad, String unidad, BigDecimal mermaPct,
            boolean opcional) {
        Receta r = new Receta();
        r.id = UUID.randomUUID();
        r.negocioId = negocioId;
        r.itemMenuId = itemMenuId;
        r.productoId = productoId;
        r.aplicar(nombreSnapshot, cantidad, unidad, mermaPct, opcional);
        return r;
    }

    public void editar(String nombreSnapshot, BigDecimal cantidad, String unidad,
            BigDecimal mermaPct, boolean opcional) {
        aplicar(nombreSnapshot, cantidad, unidad, mermaPct, opcional);
    }

    private void aplicar(String nombreSnapshot, BigDecimal cantidad, String unidad,
            BigDecimal mermaPct, boolean opcional) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de la receta debe ser mayor que cero");
        }
        BigDecimal merma = mermaPct == null || mermaPct.signum() < 0 ? BigDecimal.ZERO : mermaPct;
        if (merma.compareTo(BigDecimal.ONE) >= 0) {
            throw new ReglaDeNegocioException("La merma va entre 0 y 1 (0.10 = 10 %)");
        }
        this.nombreSnapshot = nombreSnapshot == null || nombreSnapshot.isBlank() ? null
                : nombreSnapshot.trim();
        this.cantidad = cantidad;
        this.unidad = unidad == null || unidad.isBlank() ? null : unidad.trim();
        this.mermaPct = merma;
        this.opcional = opcional;
    }

    /** Lo que se consume de verdad por unidad del ítem: {@code cantidad × (1 + merma)}. */
    public BigDecimal cantidadConMerma() {
        return cantidad.multiply(BigDecimal.ONE.add(mermaPct)).setScale(6, RoundingMode.HALF_UP);
    }

    /** El costo que aporta esta línea al ítem, dado el costo unitario del insumo. */
    public BigDecimal costoCon(BigDecimal costoUnitario) {
        if (costoUnitario == null || costoUnitario.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return cantidadConMerma().multiply(costoUnitario).setScale(4, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getItemMenuId() {
        return itemMenuId;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public String getNombreSnapshot() {
        return nombreSnapshot;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getUnidad() {
        return unidad;
    }

    public BigDecimal getMermaPct() {
        return mermaPct;
    }

    public boolean isOpcional() {
        return opcional;
    }
}
