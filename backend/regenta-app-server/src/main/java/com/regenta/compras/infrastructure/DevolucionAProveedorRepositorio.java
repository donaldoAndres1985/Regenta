package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.DevolucionAProveedor;

public interface DevolucionAProveedorRepositorio extends JpaRepository<DevolucionAProveedor, UUID> {

    Optional<DevolucionAProveedor> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<DevolucionAProveedor> findByNegocioIdOrderByFechaDesc(UUID negocioId);

    List<DevolucionAProveedor> findByNegocioIdAndRecepcionIdOrderByFechaDesc(UUID negocioId, UUID recepcionId);
}
