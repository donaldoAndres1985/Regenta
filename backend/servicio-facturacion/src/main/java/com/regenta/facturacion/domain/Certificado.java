package com.regenta.facturacion.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Metadatos del certificado de firma. El archivo {@code .p12} NUNCA está aquí:
 * va a un gestor de secretos y en {@code referenciaKms} solo queda su clave
 * (HU-055 criterio 6).
 */
@Entity
@Table(name = "certificados")
public class Certificado {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 80)
    private String alias;

    @Column(length = 150)
    private String emisor;

    @Column(name = "numero_serie", length = 80)
    private String numeroSerie;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDate vigenteHasta;

    @Column(name = "referencia_kms", nullable = false, columnDefinition = "text")
    private String referenciaKms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCertificado estado;

    protected Certificado() {
    }

    public static Certificado registrar(UUID negocioId, String alias, String emisor,
            String numeroSerie, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String referenciaKms) {
        if (referenciaKms == null || referenciaKms.isBlank()) {
            throw new ReglaDeNegocioException(
                    "El certificado se guarda por referencia al gestor de secretos, no el archivo");
        }
        if (vigenteHasta == null || vigenteDesde == null || vigenteHasta.isBefore(vigenteDesde)) {
            throw new ReglaDeNegocioException("La vigencia del certificado termina antes de empezar");
        }
        Certificado c = new Certificado();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.alias = alias;
        c.emisor = emisor;
        c.numeroSerie = numeroSerie;
        c.vigenteDesde = vigenteDesde;
        c.vigenteHasta = vigenteHasta;
        c.referenciaKms = referenciaKms;
        c.estado = EstadoCertificado.ACTIVO;
        return c;
    }

    /** Criterio 5: activo y a menos de {@code dias} de vencer. */
    public boolean porVencer(LocalDate hoy, int dias) {
        return estado == EstadoCertificado.ACTIVO
                && !vigenteHasta.isAfter(hoy.plusDays(dias));
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        return estado == EstadoCertificado.ACTIVO
                && !fecha.isBefore(vigenteDesde) && !fecha.isAfter(vigenteHasta);
    }

    public void revocar() {
        this.estado = EstadoCertificado.REVOCADO;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getAlias() {
        return alias;
    }

    public String getEmisor() {
        return emisor;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public String getReferenciaKms() {
        return referenciaKms;
    }

    public EstadoCertificado getEstado() {
        return estado;
    }
}
