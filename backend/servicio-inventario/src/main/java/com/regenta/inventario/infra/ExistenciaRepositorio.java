package com.regenta.inventario.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.ExistenciaId;

import jakarta.persistence.LockModeType;

public interface ExistenciaRepositorio extends JpaRepository<Existencia, ExistenciaId> {

    List<Existencia> findByProductoIdOrderByBodegaId(UUID productoId);

    List<Existencia> findByBodegaId(UUID bodegaId);

    Optional<Existencia> findByProductoIdAndBodegaId(UUID productoId, UUID bodegaId);

    boolean existsByBodegaId(UUID bodegaId);

    /**
     * Bloquea la fila de existencia para el resto de la transacción: es lo que
     * evita que dos reservas concurrentes vean las dos la última unidad
     * disponible (HU-034, criterio 4).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Existencia e where e.productoId = :productoId and e.bodegaId = :bodegaId")
    Optional<Existencia> bloquearPorProductoYBodega(@Param("productoId") UUID productoId,
            @Param("bodegaId") UUID bodegaId);
}
