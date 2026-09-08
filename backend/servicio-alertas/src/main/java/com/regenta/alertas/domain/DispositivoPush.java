package com.regenta.alertas.domain;

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
 * Un token FCM de un usuario (HU-094). El push crítico va a los dispositivos
 * activos de plataforma ANDROID/IOS (criterio 1); un token que la pasarela
 * rechaza deja el dispositivo inactivo (criterio 5).
 */
@Entity
@Table(name = "dispositivos_push")
public class DispositivoPush {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_fcm", nullable = false, length = 255)
    private String tokenFcm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlataformaDispositivo plataforma;

    @Column(length = 80)
    private String modelo;

    @Column(name = "version_app", length = 20)
    private String versionApp;

    @Column(nullable = false)
    private boolean activo;

    @CreationTimestamp
    @Column(name = "registrado_en", nullable = false, updatable = false)
    private OffsetDateTime registradoEn;

    @Column(name = "ultimo_uso_en")
    private OffsetDateTime ultimoUsoEn;

    protected DispositivoPush() {
    }

    public static DispositivoPush registrar(UUID negocioId, UUID usuarioId, String tokenFcm,
            PlataformaDispositivo plataforma, String modelo, String versionApp) {
        DispositivoPush d = new DispositivoPush();
        d.id = UUID.randomUUID();
        d.negocioId = negocioId;
        d.usuarioId = usuarioId;
        d.tokenFcm = tokenFcm;
        d.plataforma = plataforma;
        d.modelo = modelo;
        d.versionApp = versionApp;
        d.activo = true;
        d.ultimoUsoEn = OffsetDateTime.now();
        return d;
    }

    /** El mismo token vuelve a registrarse: puede haber cambiado de dueño o de app. */
    public void reasignar(UUID usuarioId, PlataformaDispositivo plataforma, String modelo,
            String versionApp) {
        this.usuarioId = usuarioId;
        this.plataforma = plataforma;
        this.modelo = modelo;
        this.versionApp = versionApp;
        this.activo = true;
        this.ultimoUsoEn = OffsetDateTime.now();
    }

    public void desactivar() {
        this.activo = false;
    }

    public boolean recibePush() {
        return activo && (plataforma == PlataformaDispositivo.ANDROID
                || plataforma == PlataformaDispositivo.IOS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getTokenFcm() {
        return tokenFcm;
    }

    public PlataformaDispositivo getPlataforma() {
        return plataforma;
    }

    public boolean isActivo() {
        return activo;
    }
}
