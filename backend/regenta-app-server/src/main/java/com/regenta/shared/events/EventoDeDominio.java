package com.regenta.shared.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contrato base para eventos de dominio en el monolito modular de Regenta.
 *
 * <p>Todo evento propaga explícitamente el {@code negocioId} para garantizar que
 * los listeners asíncronos en segundo plano puedan reconstruir el contexto del tenant
 * sin depender de un ThreadLocal del hilo HTTP emisor.
 */
public interface EventoDeDominio {
    UUID negocioId();
    OffsetDateTime ocurridoEn();
}
