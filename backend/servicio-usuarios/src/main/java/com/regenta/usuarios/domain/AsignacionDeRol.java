package com.regenta.usuarios.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Un rol que tiene un usuario, opcionalmente acotado a una sucursal.
 *
 * <p>Sin sucursal, el rol vale en todas. La tabla lo materializa en una columna
 * generada para poder meterlo en la clave primaria; aqui no hace falta verla.
 */
@Embeddable
public class AsignacionDeRol implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "rol_id", nullable = false)
    private UUID rolId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    protected AsignacionDeRol() {
    }

    public AsignacionDeRol(UUID rolId, UUID sucursalId) {
        this.rolId = rolId;
        this.sucursalId = sucursalId;
    }

    public static AsignacionDeRol enTodaSucursal(UUID rolId) {
        return new AsignacionDeRol(rolId, null);
    }

    public UUID getRolId() {
        return rolId;
    }

    public UUID getSucursalId() {
        return sucursalId;
    }

    public boolean valeEnTodaSucursal() {
        return sucursalId == null;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof AsignacionDeRol asignacion)) {
            return false;
        }
        return Objects.equals(rolId, asignacion.rolId)
                && Objects.equals(sucursalId, asignacion.sucursalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolId, sucursalId);
    }
}
