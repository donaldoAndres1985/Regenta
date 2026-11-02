package com.regenta.clientes.api;

import java.math.BigDecimal;
import java.util.List;
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
            String regimenFiscal,
            String digitoVerificacion,
            List<String> responsabilidadesFiscales
    ) {}

    record CarteraInfo(
            UUID clienteId,
            boolean creditoHabilitado,
            BigDecimal cupo,
            BigDecimal saldo,
            BigDecimal disponible,
            int diasCredito,
            boolean carteraVencida,
            long diasMoraMaxima
    ) {}

    Optional<ClienteInfo> obtenerCliente(UUID clienteId);

    Optional<CarteraInfo> obtenerCartera(UUID clienteId);
}
