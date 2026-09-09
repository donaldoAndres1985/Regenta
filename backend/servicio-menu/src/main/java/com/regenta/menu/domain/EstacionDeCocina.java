package com.regenta.menu.domain;

import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un puesto de la cocina (HU-077): parrilla, fría, bar, postres. Cada ítem se
 * prepara en una, así la comanda llega a donde corresponde. El {@code codigo} es
 * único por negocio ({@code uq_estacion}).
 */
@Entity
@Table(name = "estaciones_cocina")
public class EstacionDeCocina {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 80)
    private String impresora;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activa;

    protected EstacionDeCocina() {
    }

    public static EstacionDeCocina crear(UUID negocioId, UUID sucursalId, String codigo,
            String nombre, String impresora, int orden) {
        EstacionDeCocina e = new EstacionDeCocina();
        e.id = UUID.randomUUID();
        e.negocioId = negocioId;
        e.sucursalId = sucursalId;
        e.codigo = codigo.trim().toUpperCase(Locale.ROOT);
        e.activa = true;
        e.aplicar(nombre, impresora, orden);
        return e;
    }

    public void editar(String nombre, String impresora, int orden) {
        aplicar(nombre, impresora, orden);
    }

    private void aplicar(String nombre, String impresora, int orden) {
        this.nombre = nombre;
        this.impresora = impresora == null || impresora.isBlank() ? null : impresora.trim();
        this.orden = Math.max(0, orden);
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getImpresora() {
        return impresora;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActiva() {
        return activa;
    }
}
