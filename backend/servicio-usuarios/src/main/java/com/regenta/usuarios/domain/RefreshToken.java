package com.regenta.usuarios.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un refresh token vivo. Del token en claro solo queda su SHA-256: si alguien
 * se lleva esta tabla, no se lleva ni una sesion.
 *
 * <p>Cada uso rota: el anterior queda revocado y apuntando al que lo
 * reemplazo. Esa cadena es lo que permite detectar un robo -- si alguien
 * presenta uno ya usado, es que hay dos manos con el mismo token.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, length = 128, updatable = false)
    private String tokenHash;

    @Column(name = "dispositivo_id", length = 80)
    private String dispositivoId;

    @Column(length = 20)
    private String plataforma;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "emitido_en", nullable = false, updatable = false)
    private OffsetDateTime emitidoEn;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;

    @Column(name = "reemplazado_por")
    private UUID reemplazadoPor;

    protected RefreshToken() {
    }

    public static RefreshToken nuevo(UUID negocioId, UUID usuarioId, String tokenHash,
            String dispositivoId, String plataforma, String userAgent, OffsetDateTime expiraEn) {
        RefreshToken token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.negocioId = negocioId;
        token.usuarioId = usuarioId;
        token.tokenHash = tokenHash;
        token.dispositivoId = dispositivoId;
        token.plataforma = plataforma;
        token.userAgent = userAgent;
        token.expiraEn = expiraEn;
        return token;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getDispositivoId() {
        return dispositivoId;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public OffsetDateTime getEmitidoEn() {
        return emitidoEn;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public OffsetDateTime getRevocadoEn() {
        return revocadoEn;
    }

    public UUID getReemplazadoPor() {
        return reemplazadoPor;
    }

    public boolean estaVivo(OffsetDateTime ahora) {
        return revocadoEn == null && expiraEn.isAfter(ahora);
    }

    public boolean yaSeUso() {
        return revocadoEn != null;
    }

    public void revocar(OffsetDateTime cuando) {
        if (revocadoEn == null) {
            this.revocadoEn = cuando;
        }
    }

    public void rotarHacia(UUID nuevo, OffsetDateTime cuando) {
        revocar(cuando);
        this.reemplazadoPor = nuevo;
    }
}
