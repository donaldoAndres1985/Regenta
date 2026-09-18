package com.regenta.reportes.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.reportes.domain.DimProducto;

public interface DimProductoRepositorio extends JpaRepository<DimProducto, Long> {

    Optional<DimProducto> findByNegocioIdAndProductoIdAndEsActualTrue(UUID negocioId, UUID productoId);
}
