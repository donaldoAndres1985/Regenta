package com.regenta.usuarios.api;

import java.util.UUID;

/**
 * Consulta de políticas y configuración del negocio para módulos internos.
 */
public interface ConfiguracionNegocioApi {

    /**
     * Indica si el negocio permite registrar salidas/ventas con stock en 0 o negativo.
     */
    boolean permiteStockNegativo(UUID negocioId);

    /**
     * Retorna los datos básicos del negocio para el tiquete o comprobante de venta.
     */
    DatosNegocioTicket datosTicket(UUID negocioId);

    record DatosNegocioTicket(
        String razonSocial,
        String nombreComercial,
        String nit,
        String direccion,
        String telefono,
        String email,
        String ciudad
    ) {}
}
