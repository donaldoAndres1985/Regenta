package com.regenta.usuarios.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Por donde empieza el login: a que negocios pertenece un correo.
 *
 * <p>Sin RLS a proposito (ver V4): se consulta antes de saber el tenant. No
 * guarda hash, ni estado, ni roles; solo la correspondencia. La verdad sigue
 * estando en {@link Usuario}.
 */
@Entity
@Table(name = "acceso_por_correo")
public class AccesoPorCorreo {

    @EmbeddedId
    private ClaveDeAcceso clave;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    protected AccesoPorCorreo() {
    }

    public AccesoPorCorreo(String email, UUID negocioId, UUID usuarioId, String nombreComercial) {
        this.clave = new ClaveDeAcceso(Usuario.normalizar(email), negocioId);
        this.usuarioId = usuarioId;
        this.nombreComercial = nombreComercial;
    }

    public String getEmail() {
        return clave.getEmail();
    }

    public UUID getNegocioId() {
        return clave.getNegocioId();
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }
}
