package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una invitacion para entrar a un negocio. HU-015.
 *
 * <p>Del token en claro solo queda su SHA-256, igual que con los refresh
 * tokens. Y expira: una invitacion que no caduca es una puerta abierta con la
 * llave puesta.
 */
@Entity
@Table(name = "invitaciones")
public class Invitacion {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ACEPTADA = "ACEPTADA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String REVOCADA = "REVOCADA";

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "rol_id", nullable = false)
    private UUID rolId;

    @Column(name = "token_hash", nullable = false, length = 128, updatable = false)
    private String tokenHash;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "invitado_por")
    private UUID invitadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "aceptada_en")
    private OffsetDateTime aceptadaEn;

    protected Invitacion() {
    }

    public static Invitacion pendiente(UUID negocioId, String email, UUID rolId, String tokenHash,
            UUID invitadoPor, OffsetDateTime expiraEn) {
        Invitacion invitacion = new Invitacion();
        invitacion.id = UUID.randomUUID();
        invitacion.negocioId = negocioId;
        invitacion.email = Usuario.normalizar(email);
        invitacion.rolId = rolId;
        invitacion.tokenHash = tokenHash;
        invitacion.estado = PENDIENTE;
        invitacion.invitadoPor = invitadoPor;
        invitacion.expiraEn = expiraEn;
        return invitacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getEmail() {
        return email;
    }

    public UUID getRolId() {
        return rolId;
    }

    public String getEstado() {
        return estado;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public boolean estaPendiente() {
        return PENDIENTE.equals(estado);
    }

    public boolean vencio(OffsetDateTime ahora) {
        return expiraEn.isBefore(ahora);
    }

    public void aceptar(OffsetDateTime cuando) {
        this.estado = ACEPTADA;
        this.aceptadaEn = cuando;
    }

    public void marcarVencida() {
        this.estado = EXPIRADA;
    }

    public void revocar() {
        this.estado = REVOCADA;
    }
}
