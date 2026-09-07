package com.regenta.inventario.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.inventario.domain.Producto;

public interface ProductoRepositorio extends JpaRepository<Producto, UUID> {

    boolean existsByNegocioIdAndSku(UUID negocioId, String sku);

    boolean existsByNegocioIdAndSkuAndIdNot(UUID negocioId, String sku, UUID id);

    boolean existsByNegocioIdAndCodigoBarras(UUID negocioId, String codigoBarras);

    boolean existsByNegocioIdAndCodigoBarrasAndIdNot(UUID negocioId, String codigoBarras, UUID id);

    Optional<Producto> findByNegocioIdAndCodigoBarras(UUID negocioId, String codigoBarras);

    List<Producto> findByNegocioIdAndActivoTrueOrderByNombreAsc(UUID negocioId);

    /**
     * Búsqueda del vendedor (HU-035): por nombre, SKU, código de barras propio o
     * un código alterno. El {@code termino} llega ya en minúsculas y con los
     * comodines {@code %…%}.
     */
    @Query("""
            select p from Producto p
            where p.negocioId = :negocioId and p.activo = true
              and ( lower(p.nombre) like :termino
                    or lower(p.sku) like :termino
                    or (p.codigoBarras is not null and lower(p.codigoBarras) like :termino)
                    or exists (select 1 from ProductoCodigo c
                               where c.productoId = p.id and lower(c.codigo) like :termino) )
            order by p.nombre asc
            """)
    List<Producto> buscar(@Param("negocioId") UUID negocioId, @Param("termino") String termino,
            Pageable limite);
}
