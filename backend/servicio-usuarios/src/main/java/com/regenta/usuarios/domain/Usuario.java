package com.regenta.usuarios.domain;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Una persona dentro de UN negocio.
 *
 * <p>El correo es unico por negocio, no globalmente: la misma persona puede
 * trabajar en dos negocios clientes de Regenta con el mismo correo, y son dos
 * usuarios distintos con contraseñas distintas.
 *
 * <p>Un usuario nunca se borra: se desactiva. Sus ventas siguen atribuidas a el.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final String INVITADO = "INVITADO";
    public static final String ACTIVO = "ACTIVO";
    public static final String INACTIVO = "INACTIVO";
    public static final String BLOQUEADO = "BLOQUEADO";

    /** A la sexta seguida se bloquea la cuenta. */
    public static final int INTENTOS_ANTES_DE_BLOQUEAR = 5;

    public static final Duration BLOQUEO = Duration.ofMinutes(15);

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 80)
    private String apellido;

    @Column(length = 30)
    private String documento;

    @Column(length = 30)
    private String telefono;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "mfa_habilitado", nullable = false)
    private boolean mfaHabilitado;

    @Column(name = "intentos_fallidos", nullable = false)
    private short intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "password_cambiado_en")
    private OffsetDateTime passwordCambiadoEn;

    @Column(name = "ultimo_acceso_en")
    private OffsetDateTime ultimoAccesoEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    private Set<AsignacionDeRol> roles = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_sucursales", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "sucursal_id", nullable = false)
    private Set<UUID> sucursales = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Column(name = "eliminado_en")
    private OffsetDateTime eliminadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Usuario() {
    }

    private Usuario(UUID negocioId, String email, String passwordHash, String nombre,
            String apellido, String estado) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.email = normalizar(email);
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.apellido = apellido;
        this.estado = estado;
    }

    public static Usuario activo(UUID negocioId, String email, String passwordHash, String nombre,
            String apellido) {
        return new Usuario(negocioId, email, passwordHash, nombre, apellido, ACTIVO);
    }

    /** Invitado: existe, pero no entra hasta que acepte y ponga contraseña. */
    public static Usuario invitado(UUID negocioId, String email, String nombre, String apellido) {
        return new Usuario(negocioId, email, "sin-clave", nombre, apellido, INVITADO);
    }

    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEstado() {
        return estado;
    }

    public short getIntentosFallidos() {
        return intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public OffsetDateTime getUltimoAccesoEn() {
        return ultimoAccesoEn;
    }

    public Set<AsignacionDeRol> getRoles() {
        return Set.copyOf(roles);
    }

    public Set<UUID> getSucursales() {
        return Set.copyOf(sucursales);
    }

    public long getVersion() {
        return version;
    }

    public boolean puedeEntrar() {
        return ACTIVO.equals(estado) && eliminadoEn == null;
    }

    public boolean estaBloqueado(OffsetDateTime ahora) {
        return BLOQUEADO.equals(estado)
                || (bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora));
    }

    public void asignarRol(AsignacionDeRol asignacion) {
        roles.add(asignacion);
    }

    public void quitarRoles() {
        roles.clear();
    }

    public void acotarASucursales(Collection<UUID> nuevas) {
        sucursales.clear();
        sucursales.addAll(nuevas);
    }

    public void actualizarPerfil(String nombre, String apellido, String documento, String telefono) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.documento = documento;
        this.telefono = telefono;
    }

    public void cambiarPassword(String passwordHash, OffsetDateTime cuando) {
        this.passwordHash = passwordHash;
        this.passwordCambiadoEn = cuando;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    /** El invitado acepta: pone clave y pasa a ACTIVO. */
    public void activarConPassword(String passwordHash, OffsetDateTime cuando) {
        cambiarPassword(passwordHash, cuando);
        this.estado = ACTIVO;
    }

    public void desactivar() {
        this.estado = INACTIVO;
    }

    public void reactivar() {
        this.estado = ACTIVO;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    /** Devuelve true si este fallo fue el que bloqueo la cuenta. */
    public boolean fallo(OffsetDateTime ahora) {
        intentosFallidos = (short) (intentosFallidos + 1);
        if (intentosFallidos >= INTENTOS_ANTES_DE_BLOQUEAR) {
            bloqueadoHasta = ahora.plus(BLOQUEO);
            return true;
        }
        return false;
    }

    public void entro(OffsetDateTime ahora) {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
        this.ultimoAccesoEn = ahora;
    }
}
