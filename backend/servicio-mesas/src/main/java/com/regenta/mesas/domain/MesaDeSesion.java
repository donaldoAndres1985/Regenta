package com.regenta.mesas.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Una mesa que se unió a una sesión de grupo (HU-083). La mesa principal vive en
 * {@code sesiones_mesa.mesa_principal_id}; aquí van las demás, para que diez
 * personas en M1+M2 compartan una sola sesión y una sola comanda.
 */
@Entity
@Table(name = "sesion_mesas")
@IdClass(MesaDeSesionId.class)
public class MesaDeSesion {

    @Id
    @Column(name = "sesion_id", nullable = false, updatable = false)
    private UUID sesionId;

    @Id
    @Column(name = "mesa_id", nullable = false, updatable = false)
    private UUID mesaId;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    protected MesaDeSesion() {
    }

    public static MesaDeSesion de(UUID negocioId, UUID sesionId, UUID mesaId) {
        MesaDeSesion m = new MesaDeSesion();
        m.negocioId = negocioId;
        m.sesionId = sesionId;
        m.mesaId = mesaId;
        return m;
    }

    public UUID getSesionId() {
        return sesionId;
    }

    public UUID getMesaId() {
        return mesaId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }
}
