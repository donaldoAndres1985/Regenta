package com.regenta.menu.domain;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * El estado de un ítem para un día de servicio (HU-080). El jefe de cocina lo
 * marca agotado y desaparece de la carta del mesero hasta que se reponga
 * (criterio 1). Cada fecha es una fila aparte, así que lo agotado ayer no
 * arrastra a hoy (criterio 2). Con cupo diario, el ítem se marca agotado solo al
 * llegar a la cuenta (criterio 4).
 */
@Entity
@Table(name = "disponibilidad_diaria")
@IdClass(DisponibilidadDiariaId.class)
public class DisponibilidadDiaria {

    @Id
    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Id
    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    /** Cupo del día. {@code null} = sin límite. */
    @Column(name = "cantidad_disponible")
    private Integer cantidadDisponible;

    @Column(name = "cantidad_vendida", nullable = false)
    private int cantidadVendida;

    @Column(nullable = false)
    private boolean agotado;

    protected DisponibilidadDiaria() {
    }

    public static DisponibilidadDiaria delDia(UUID negocioId, UUID itemId, LocalDate fecha) {
        DisponibilidadDiaria d = new DisponibilidadDiaria();
        d.negocioId = negocioId;
        d.itemId = itemId;
        d.fecha = fecha;
        d.cantidadVendida = 0;
        d.agotado = false;
        return d;
    }

    /** El jefe de cocina lo marca agotado a mano (criterio 1). */
    public void marcarAgotado() {
        this.agotado = true;
    }

    /** Vuelve a la carta: se repuso o se corrigió el "se acabó". */
    public void reponer() {
        this.agotado = false;
    }

    /**
     * Fija (o quita, con {@code null}) el cupo del día. Devuelve {@code true} si al
     * hacerlo el ítem quedó agotado y antes no lo estaba (para notificar una vez).
     */
    public boolean fijarCupo(Integer cupo) {
        boolean estabaAgotado = agotado;
        this.cantidadDisponible = cupo == null || cupo < 0 ? null : cupo;
        reevaluarCupo();
        return agotado && !estabaAgotado;
    }

    /**
     * Suma ventas del ítem en el día. Devuelve {@code true} si esta venta es la que
     * lo dejó agotado por cupo, para publicar el evento una sola vez (criterio 4).
     */
    public boolean registrarVenta(int cantidad) {
        if (cantidad <= 0) {
            return false;
        }
        boolean estabaAgotado = agotado;
        this.cantidadVendida += cantidad;
        reevaluarCupo();
        return agotado && !estabaAgotado;
    }

    private void reevaluarCupo() {
        if (cantidadDisponible != null && cantidadVendida >= cantidadDisponible) {
            this.agotado = true;
        }
    }

    public boolean estaAgotado() {
        return agotado;
    }

    public UUID getItemId() {
        return itemId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public Integer getCantidadDisponible() {
        return cantidadDisponible;
    }

    public int getCantidadVendida() {
        return cantidadVendida;
    }
}
