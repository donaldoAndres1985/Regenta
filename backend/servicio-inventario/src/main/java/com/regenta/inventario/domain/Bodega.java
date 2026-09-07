package com.regenta.inventario.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un sitio donde hay mercancia. El stock es {@code (producto, bodega)}, nunca
 * una columna de {@code productos}: el plan Empresarial vende multi-sucursal y
 * POS necesita saber de que bodega salio lo que se vendio.
 */
@Entity
@Table(name = "bodegas")
public class Bodega {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoBodega tipo;

    @Column(name = "es_default", nullable = false)
    private boolean esDefault;

    @Column(nullable = false)
    private boolean activa;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Bodega() {
    }

    public static Bodega nueva(UUID negocioId, String codigo, String nombre, TipoBodega tipo,
            boolean esDefault) {
        Bodega bodega = new Bodega();
        bodega.id = UUID.randomUUID();
        bodega.negocioId = negocioId;
        bodega.codigo = codigo;
        bodega.nombre = nombre;
        bodega.tipo = tipo == null ? TipoBodega.SECUNDARIA : tipo;
        bodega.esDefault = esDefault;
        bodega.activa = true;
        return bodega;
    }

    public static Bodega principalPorDefecto(UUID negocioId) {
        return nueva(negocioId, "BOD-PRAL", "Bodega principal", TipoBodega.PRINCIPAL, true);
    }

    /** La bodega donde vive la mercancia mientras viaja de una sede a otra (HU-032). */
    public static Bodega deTransito(UUID negocioId) {
        return nueva(negocioId, "BOD-TRANS", "Transito", TipoBodega.TRANSITO, false);
    }

    public void renombrar(String nombre, UUID sucursalId) {
        this.nombre = nombre;
        this.sucursalId = sucursalId;
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

    public TipoBodega getTipo() {
        return tipo;
    }

    public boolean isEsDefault() {
        return esDefault;
    }

    public boolean isActiva() {
        return activa;
    }
}
