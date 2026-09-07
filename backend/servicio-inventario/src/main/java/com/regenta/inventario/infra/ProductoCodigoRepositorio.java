package com.regenta.inventario.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.ProductoCodigo;

public interface ProductoCodigoRepositorio extends JpaRepository<ProductoCodigo, UUID> {

    Optional<ProductoCodigo> findByNegocioIdAndCodigo(UUID negocioId, String codigo);

    boolean existsByNegocioIdAndCodigo(UUID negocioId, String codigo);
}
