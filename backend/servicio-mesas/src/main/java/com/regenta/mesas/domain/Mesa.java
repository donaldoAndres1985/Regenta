package com.regenta.mesas.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una mesa del salón (HU-081): su código, su capacidad, la zona a la que
 * pertenece y su posición en el plano (criterio 1). El código es único por
 * negocio (criterio 2). Mover la mesa guarda la posición y se ve igual la
 * próxima vez (criterio 3).
 */
@Entity
@Table(name = "mesas")
public class Mesa {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "zona_id")
    private UUID zonaId;

    @Column(nullable = false, length = 20, updatable = false)
    private String codigo;

    @Column(length = 60)
    private String nombre;

    @Column(nullable = false)
    private short capacidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaDeMesa forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDeMesa estado;

    @Column(name = "pos_x", nullable = false)
    private int posX;

    @Column(name = "pos_y", nullable = false)
    private int posY;

    @Column(nullable = false)
    private int ancho;

    @Column(nullable = false)
    private int alto;

    @Column(name = "qr_token", length = 64)
    private String qrToken;

    @Column(nullable = false)
    private boolean activa;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Mesa() {
    }

    public static Mesa crear(UUID negocioId, UUID sucursalId, UUID zonaId, String codigo,
            String nombre, Integer capacidad, FormaDeMesa forma, Integer posX, Integer posY,
            Integer ancho, Integer alto) {
        Mesa m = new Mesa();
        m.id = UUID.randomUUID();
        m.negocioId = negocioId;
        m.sucursalId = sucursalId;
        m.codigo = exigirCodigo(codigo);
        m.estado = EstadoDeMesa.LIBRE;
        m.activa = true;
        m.posX = 0;
        m.posY = 0;
        m.ancho = 80;
        m.alto = 80;
        m.editar(zonaId, nombre, capacidad, forma);
        m.mover(posX, posY, ancho, alto);
        return m;
    }

    public void editar(UUID zonaId, String nombre, Integer capacidad, FormaDeMesa forma) {
        this.zonaId = zonaId;
        this.nombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        if (capacidad != null) {
            if (capacidad <= 0) {
                throw new ReglaDeNegocioException("La capacidad de la mesa debe ser mayor que cero");
            }
            this.capacidad = capacidad.shortValue();
        } else if (this.capacidad == 0) {
            this.capacidad = 4;
        }
        this.forma = forma == null ? FormaDeMesa.CUADRADA : forma;
    }

    /** Guarda la posición y el tamaño en el plano (HU-081 criterio 3). */
    public void mover(Integer posX, Integer posY, Integer ancho, Integer alto) {
        if (posX != null) {
            this.posX = Math.max(0, posX);
        }
        if (posY != null) {
            this.posY = Math.max(0, posY);
        }
        if (ancho != null) {
            this.ancho = Math.max(1, ancho);
        }
        if (alto != null) {
            this.alto = Math.max(1, alto);
        }
    }

    public void activar() {
        this.activa = true;
    }

    public void desactivar() {
        this.activa = false;
    }

    /** Al abrir una sesión (HU-082 criterio 1). Desde libre o reservada. */
    public void ocupar() {
        this.estado = EstadoDeMesa.OCUPADA;
    }

    /** El comensal pide la cuenta (HU-082): sigue ocupada pero ya no admite más. */
    public void pedirCuenta() {
        this.estado = EstadoDeMesa.CUENTA_PEDIDA;
    }

    /** Al cerrar la comanda, la mesa queda «por limpiar», no libre (HU-082 criterio 3). */
    public void ensuciar() {
        this.estado = EstadoDeMesa.SUCIA;
    }

    /** El mesero la marca limpia (HU-082 criterio 4). Solo desde «por limpiar». */
    public void limpiar() {
        if (estado != EstadoDeMesa.SUCIA) {
            throw new ReglaDeNegocioException(
                    "Solo se marca limpia una mesa que está por limpiar");
        }
        this.estado = EstadoDeMesa.LIBRE;
    }

    private static String exigirCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaDeNegocioException("La mesa necesita un código");
        }
        return codigo.trim().toUpperCase();
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

    public UUID getZonaId() {
        return zonaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public short getCapacidad() {
        return capacidad;
    }

    public FormaDeMesa getForma() {
        return forma;
    }

    public EstadoDeMesa getEstado() {
        return estado;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public String getQrToken() {
        return qrToken;
    }

    public boolean isActiva() {
        return activa;
    }

    public long getVersion() {
        return version;
    }
}
