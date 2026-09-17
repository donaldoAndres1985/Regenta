package com.regenta.comandas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.Cuenta;

public interface CuentaRepositorio extends JpaRepository<Cuenta, UUID> {

    List<Cuenta> findByComandaIdOrderByNumeroDivisionAsc(UUID comandaId);

    Optional<Cuenta> findByIdAndNegocioId(UUID id, UUID negocioId);

    int countByComandaId(UUID comandaId);
}
