package com.regenta.facturacion.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un periodo en el que la DIAN no respondió y se siguió facturando en
 * contingencia (HU-057). El contador {@code facturasAfectadas} sube por SQL
 * atómico; cerrarla ({@code finEn} + {@code regularizada}) dispara la
 * retransmisión de lo pendiente.
 */
@Entity
@Table(name = "contingencias")
public class Contingencia {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "inicio_en", nullable = false, updatable = false)
    private OffsetDateTime inicioEn;

    @Column(name = "fin_en")
    private OffsetDateTime finEn;

    @Column(columnDefinition = "text")
    private String motivo;

    @Column(name = "facturas_afectadas", nullable = false)
    private int facturasAfectadas;

    @Column(nullable = false)
    private boolean regularizada;

    protected Contingencia() {
    }

    public static Contingencia abrir(UUID negocioId, String motivo) {
        Contingencia c = new Contingencia();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.inicioEn = OffsetDateTime.now();
        c.motivo = motivo == null || motivo.isBlank() ? "La DIAN no respondió tras los reintentos"
                : motivo;
        c.facturasAfectadas = 0;
        c.regularizada = false;
        return c;
    }

    public void cerrar() {
        if (finEn == null) {
            this.finEn = OffsetDateTime.now();
            this.regularizada = true;
        }
    }

    public boolean estaAbierta() {
        return finEn == null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public OffsetDateTime getInicioEn() {
        return inicioEn;
    }

    public OffsetDateTime getFinEn() {
        return finEn;
    }

    public String getMotivo() {
        return motivo;
    }

    public int getFacturasAfectadas() {
        return facturasAfectadas;
    }

    public boolean isRegularizada() {
        return regularizada;
    }
}
