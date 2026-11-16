package com.regenta.caja.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público del módulo caja para otros módulos.
 */
public interface CajaApi {

    /**
     * HU-146 · la sesión de caja abierta por ese usuario en ese negocio, si la hay. Es la caja a
     * la que entra el dinero de lo que ese usuario vende.
     */
    Optional<UUID> sesionAbiertaDe(UUID negocioId, UUID usuarioId);
}
