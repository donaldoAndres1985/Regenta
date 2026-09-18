package com.regenta.reportes.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.reportes.domain.DimCliente;

public interface DimClienteRepositorio extends JpaRepository<DimCliente, Long> {

    Optional<DimCliente> findByNegocioIdAndClienteIdAndEsActualTrue(UUID negocioId, UUID clienteId);
}
