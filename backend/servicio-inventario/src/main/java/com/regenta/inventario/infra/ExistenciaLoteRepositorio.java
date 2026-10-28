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

    /**
     * Los lotes del negocio con saldo que vencen entre {@code hoy} y {@code limite},
     * con la existencia sumada de todas las bodegas (HU-138, criterio 7).
     */
    @Query("""
            select new com.regenta.inventario.aplicacion.FilaPorVencer(
                l.id, l.productoId, p.nombre, l.codigoLote, l.fechaVencimiento, sum(el.cantidad))
            from ExistenciaLote el, Lote l, Producto p
            where el.loteId = l.id
              and l.productoId = p.id
              and l.negocioId = :negocioId
              and l.fechaVencimiento between :hoy and :limite
            group by l.id, l.productoId, p.nombre, l.codigoLote, l.fechaVencimiento
            having sum(el.cantidad) > 0
            order by l.fechaVencimiento asc, l.codigoLote asc
            """)
    List<com.regenta.inventario.aplicacion.FilaPorVencer> porVencer(
            @Param("negocioId") UUID negocioId, @Param("hoy") LocalDate hoy,
            @Param("limite") LocalDate limite);
}
