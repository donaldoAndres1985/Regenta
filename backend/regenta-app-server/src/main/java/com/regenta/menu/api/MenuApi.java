package com.regenta.menu.api;

import java.util.List;
import java.util.UUID;

/**
 * Contrato público que el módulo menú expone a otros módulos (como comandas).
 */
public interface MenuApi {

    /** El ítem del menú, o null si no existe o no está activo. */
    ItemDeMenuDto item(UUID negocioId, UUID itemMenuId);

    /** Valida modificadores y devuelve opciones con su precio. */
    CotizacionModificadoresDto cotizarModificadores(UUID negocioId, UUID itemMenuId, List<UUID> modificadorIds);
}
