package com.regenta.menu.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.menu.domain.Receta;

public interface RecetaRepositorio extends JpaRepository<Receta, UUID> {

    Optional<Receta> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Receta> findByItemMenuIdOrderByNombreSnapshotAscProductoIdAsc(UUID itemMenuId);

    boolean existsByItemMenuIdAndProductoId(UUID itemMenuId, UUID productoId);

    /** Los ítems cuya receta usa ese producto (para recalcular su costo, HU-079 criterio 2). */
    @Query("select distinct r.itemMenuId from Receta r "
            + "where r.negocioId = :negocioId and r.productoId = :productoId")
    List<UUID> itemsQueUsan(@Param("negocioId") UUID negocioId,
            @Param("productoId") UUID productoId);
}
