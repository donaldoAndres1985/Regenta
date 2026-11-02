package com.regenta.clientes.api;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público del módulo de clientes para consultas síncronas
 * desde otros módulos (ventas, reservas).
 */
public interface ClientesApi {

    record ClienteInfo(
            UUID id,
            String tipoDocumento,
            String numeroDocumento,
            String nombre,
            String razonSocial,
            String email,
            String telefono,
            String direccion,
            String regimenFiscal
    ) {}

    record CarteraInfo(
            UUID clienteId,
            BigDecimal limiteCredito,
            BigDecimal saldoPendiente,
            BigDecimal saldoDisponible,
            int diasMora,
            boolean bloqueado
    ) {}

    Optional<ClienteInfo> obtenerCliente(UUID clienteId);

    Optional<CarteraInfo> obtenerCartera(UUID clienteId);
}
