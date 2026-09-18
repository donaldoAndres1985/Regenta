package com.regenta.ventas.aplicacion;

import java.util.UUID;

/**
 * El puerto hacia servicio-clientes (HU-113). Lanza
 * {@code NoEncontradoException} si el cliente no existe en el negocio de quien
 * pregunta: un cliente de otro negocio no existe, no es que esté prohibido.
 */
public interface ConsultaDeClientes {

    ClienteDeLaVenta consultar(UUID clienteId);
}
