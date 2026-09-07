package com.regenta.clientes.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.clientes.domain.CuentaPorCobrar;

public interface CuentaPorCobrarRepositorio extends JpaRepository<CuentaPorCobrar, UUID> {

    List<CuentaPorCobrar> findByNegocioIdAndClienteIdOrderByFechaVencimientoAsc(UUID negocioId,
            UUID clienteId);

    boolean existsByNegocioIdAndOrigenTipoAndOrigenId(UUID negocioId,
            com.regenta.clientes.domain.OrigenDeCuenta origenTipo, UUID origenId);

    Optional<CuentaPorCobrar> findByIdAndNegocioId(UUID id, UUID negocioId);
}
