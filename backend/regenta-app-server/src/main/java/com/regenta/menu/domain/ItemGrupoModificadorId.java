package com.regenta.menu.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Clave compuesta de {@link ItemGrupoModificador}: {@code (item_id, grupo_id)}. */
public class ItemGrupoModificadorId implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID itemId;
    private UUID grupoId;

    public ItemGrupoModificadorId() {
    }

    public ItemGrupoModificadorId(UUID itemId, UUID grupoId) {
        this.itemId = itemId;
        this.grupoId = grupoId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getGrupoId() {
        return grupoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemGrupoModificadorId otra)) {
            return false;
        }
        return Objects.equals(itemId, otra.itemId) && Objects.equals(grupoId, otra.grupoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, grupoId);
    }
}
