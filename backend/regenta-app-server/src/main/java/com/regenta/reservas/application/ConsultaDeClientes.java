package com.regenta.reservas.application;

import java.util.Optional;
import java.util.UUID;

/**
 * El puerto hacia servicio-clientes (HU-118). A diferencia de la consulta que
 * hace Ventas al asignar un cliente, esta se hace al cerrar la estancia y
 * nunca puede bloquear el check-out: si el cliente no existe o
 * servicio-clientes no responde, devuelve vacÃ­o y la factura sale al
 * adquiriente genÃ©rico (`design/comportamiento/ClienteVenta.md`, R8).
 */
public interface ConsultaDeClientes {

    Optional<ClienteDeLaReserva> consultar(UUID clienteId);
}

