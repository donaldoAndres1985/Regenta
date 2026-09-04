package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Sucursal;

public interface SucursalRepositorio extends JpaRepository<Sucursal, UUID> {

    List<Sucursal> findByNegocioIdOrderByCodigo(UUID negocioId);

    Optional<Sucursal> findByNegocioIdAndEsPrincipalTrue(UUID negocioId);

    Optional<Sucursal> findByNegocioIdAndCodigo(UUID negocioId, String codigo);

    long countByNegocioIdAndActivaTrue(UUID negocioId);
}
