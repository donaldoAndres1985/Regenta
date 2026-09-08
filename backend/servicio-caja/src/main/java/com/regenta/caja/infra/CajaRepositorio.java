package com.regenta.caja.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.caja.domain.Caja;

public interface CajaRepositorio extends JpaRepository<Caja, UUID> {

    Optional<Caja> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Caja> findByNegocioIdAndActivaTrueOrderByCodigoAsc(UUID negocioId);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
