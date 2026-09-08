package com.regenta.compras.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.EstadoSugerencia;
import com.regenta.compras.domain.SugerenciaDeCompra;

public interface SugerenciaDeCompraRepositorio extends JpaRepository<SugerenciaDeCompra, UUID> {

    Optional<SugerenciaDeCompra> findByIdAndNegocioId(UUID id, UUID negocioId);

    Optional<SugerenciaDeCompra> findByNegocioIdAndProductoIdAndEstado(UUID negocioId,
            UUID productoId, EstadoSugerencia estado);

    List<SugerenciaDeCompra> findByNegocioIdAndEstadoOrderByNombreSnapshotAsc(UUID negocioId,
            EstadoSugerencia estado);

    List<SugerenciaDeCompra> findByNegocioIdAndProveedorIdAndEstado(UUID negocioId,
            UUID proveedorId, EstadoSugerencia estado);
}
