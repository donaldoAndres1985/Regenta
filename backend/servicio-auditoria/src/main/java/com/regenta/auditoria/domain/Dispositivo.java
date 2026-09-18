package com.regenta.auditoria.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un dispositivo que sincroniza. HU-102 criterio 3. */
@Entity
@Table(name = "dispositivos")
public class Dispositivo {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, length = 120)
    private String identificador;

    @Column(length = 80)
    private String nombre;

    @Column(length = 20, nullable = false)
    private String plataforma;

    @Column(name = "version_app", length = 20)
    private String versionApp;

    @Column(name = "version_esquema_local", nullable = false)
    private int versionEsquemaLocal = 1;

    @Column(name = "ultimo_sync_en")
    private OffsetDateTime ultimoSyncEn;

    @Column(name = "cursor_sync", length = 80)
    private String cursorSync;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "registrado_en", nullable = false)
    private OffsetDateTime registradoEn;

    protected Dispositivo() {
    }

    public static Dispositivo nuevo(UUID negocioId, UUID usuarioId, String identificador, String nombre,
            String plataforma, String versionApp, OffsetDateTime ahora) {
        Dispositivo d = new Dispositivo();
        d.id = UUID.randomUUID();
        d.negocioId = negocioId;
        d.usuarioId = usuarioId;
        d.identificador = identificador;
        d.nombre = nombre;
        d.plataforma = plataforma;
        d.versionApp = versionApp;
        d.ultimoSyncEn = ahora;
        d.registradoEn = ahora;
        return d;
    }

    /** Cada lote subido dice de nuevo su plataforma y versión: puede haberse actualizado la app. */
    public void sincronizo(String plataforma, String versionApp, OffsetDateTime ahora) {
        this.plataforma = plataforma;
        this.versionApp = versionApp;
        this.ultimoSyncEn = ahora;
    }

    /** HU-104 criterio 2: solo avanza cuando la descarga terminó bien, no en cada pedido. */
    public void avanzarCursor(String cursor, OffsetDateTime ahora) {
        this.cursorSync = cursor;
        this.ultimoSyncEn = ahora;
    }

    public UUID getId() {
        return id;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public String getVersionApp() {
        return versionApp;
    }

    public OffsetDateTime getUltimoSyncEn() {
        return ultimoSyncEn;
    }

    public String getCursorSync() {
        return cursorSync;
    }
}
