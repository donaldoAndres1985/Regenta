package com.regenta.inventario.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.inventario.domain.Producto;

public interface ProductoRepositorio extends JpaRepository<Producto, UUID>, ProductoRepositorioCustom {

    boolean existsByNegocioIdAndSku(UUID negocioId, String sku);

    boolean existsByNegocioIdAndSkuAndIdNot(UUID negocioId, String sku, UUID id);

    boolean existsByNegocioIdAndCodigoBarras(UUID negocioId, String codigoBarras);

    boolean existsByNegocioIdAndCodigoBarrasAndIdNot(UUID negocioId, String codigoBarras, UUID id);

    Optional<Producto> findByNegocioIdAndCodigoBarras(UUID negocioId, String codigoBarras);

    List<Producto> findByNegocioIdAndActivoTrueOrderByNombreAsc(UUID negocioId);

    /** HU-153: todos, activos e inactivos, para el inventario general. */
    List<Producto> findByNegocioIdOrderByNombreAsc(UUID negocioId);

    /** HU-154: los productos de una categoria, para cambiarles la tarifa en bloque. */
    List<Producto> findByNegocioIdAndCategoriaId(UUID negocioId, UUID categoriaId);

    /** HU-155 C3: cuantos productos usan una unidad. */
    long countByNegocioIdAndUnidadMedidaId(UUID negocioId, UUID unidadMedidaId);

    /** HU-156: cuantos productos usan una tabla de fracciones. */
    long countByNegocioIdAndTablaFraccionesId(UUID negocioId, UUID tablaFraccionesId);

    /**
     * BÃºsqueda del vendedor (HU-035): por nombre, SKU, cÃ³digo de barras propio o
     * un cÃ³digo alterno. El {@code termino} llega ya en minÃºsculas y con los
     * comodines {@code %â€¦%}.
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
