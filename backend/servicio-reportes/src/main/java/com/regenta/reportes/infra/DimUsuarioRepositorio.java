package com.regenta.reportes.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.reportes.domain.DimUsuario;

public interface DimUsuarioRepositorio extends JpaRepository<DimUsuario, Long> {

    Optional<DimUsuario> findByNegocioIdAndUsuarioIdAndEsActualTrue(UUID negocioId, UUID usuarioId);
}
