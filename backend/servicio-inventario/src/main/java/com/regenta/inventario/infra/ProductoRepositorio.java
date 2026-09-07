package com.regenta.inventario.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.Producto;

public interface ProductoRepositorio extends JpaRepository<Producto, UUID> {

    boolean existsByNegocioIdAndSku(UUID negocioId, String sku);

    boolean existsByNegocioIdAndSkuAndIdNot(UUID negocioId, String sku, UUID id);

    boolean existsByNegocioIdAndCodigoBarras(UUID negocioId, String codigoBarras);

    boolean existsByNegocioIdAndCodigoBarrasAndIdNot(UUID negocioId, String codigoBarras, UUID id);
}
