package com.regenta.ventas.aplicacion;

import java.util.UUID;

/**
 * El cupo y la cartera de un cliente, que son de servicio-clientes (HU-022).
 * Ventas no guarda copia: pregunta en el momento de confirmar, porque entre
 * que se armó el carrito y se cobra pudo cambiar.
 */
public interface ConsultaDeCredito {

    CreditoDelCliente consultar(UUID clienteId);
}
