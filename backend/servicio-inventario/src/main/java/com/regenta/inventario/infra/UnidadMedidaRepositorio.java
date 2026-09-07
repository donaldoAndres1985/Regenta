package com.regenta.inventario.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.UnidadMedida;

public interface UnidadMedidaRepositorio extends JpaRepository<UnidadMedida, UUID> {

    List<UnidadMedida> findByNegocioIdOrderByCodigo(UUID negocioId);

    Optional<UnidadMedida> findByNegocioIdAndCodigoIgnoreCase(UUID negocioId, String codigo);
}
