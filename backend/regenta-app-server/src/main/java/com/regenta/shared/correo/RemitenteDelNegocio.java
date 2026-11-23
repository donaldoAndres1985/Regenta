package com.regenta.shared.correo;

import java.util.UUID;

/**
 * HU-122 · Con qué nombre sale el correo de un negocio y a dónde se le responde. Lo resuelve el módulo
 * de usuarios desde la configuración del negocio: no es una constante.
 */
public interface RemitenteDelNegocio {

    Remitente de(UUID negocioId);

    /** {@code responderA} nulo: el negocio no tiene correo configurado. */
    record Remitente(String nombre, String responderA) {
    }
}
