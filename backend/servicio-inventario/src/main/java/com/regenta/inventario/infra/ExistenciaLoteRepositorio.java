package com.regenta.inventario.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.inventario.aplicacion.FilaFefo;
import com.regenta.inventario.domain.ExistenciaLote;
import com.regenta.inventario.domain.ExistenciaLoteId;

public interface ExistenciaLoteRepositorio
        extends JpaRepository<ExistenciaLote, ExistenciaLoteId> {

    Optional<ExistenciaLote> findByLoteIdAndBodegaId(UUID loteId, UUID bodegaId);

    List<ExistenciaLote> findByLoteIdOrderByBodegaId(UUID loteId);

    /**
     * FEFO: los lotes de un producto con saldo en una bodega, del que vence
     * antes al que vence despues. El lote vencido queda fuera (criterio 3).
     */
    @Query("""
            select new com.regenta.inventario.aplicacion.FilaFefo(
                l.id, l.codigoLote, l.fechaVencimiento, el.cantidad)
            from ExistenciaLote el, Lote l
            where el.loteId = l.id
              and l.productoId = :productoId
              and el.bodegaId = :bodegaId
              and el.cantidad > 0
              and (l.fechaVencimiento is null or l.fechaVencimiento >= :hoy)
            order by l.fechaVencimiento asc nulls last, l.codigoLote asc
            """)
    List<FilaFefo> fefo(@Param("productoId") UUID productoId, @Param("bodegaId") UUID bodegaId,
            @Param("hoy") LocalDate hoy);
}
