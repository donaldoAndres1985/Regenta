package com.regenta.menu.domain;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Qué grupos de modificadores aplican a un ítem, y en qué orden (HU-078). */
@Entity
@Table(name = "item_grupos_modificadores")
@IdClass(ItemGrupoModificadorId.class)
public class ItemGrupoModificador {

    @Id
    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Id
    @Column(name = "grupo_id", nullable = false, updatable = false)
    private UUID grupoId;

    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(nullable = false)
    private int orden;

    protected ItemGrupoModificador() {
    }

    public static ItemGrupoModificador de(UUID negocioId, UUID itemId, UUID grupoId, int orden) {
        ItemGrupoModificador v = new ItemGrupoModificador();
        v.negocioId = negocioId;
        v.itemId = itemId;
        v.grupoId = grupoId;
        v.orden = Math.max(0, orden);
        return v;
    }

    public void reordenar(int orden) {
        this.orden = Math.max(0, orden);
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getGrupoId() {
        return grupoId;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public int getOrden() {
        return orden;
    }
}
