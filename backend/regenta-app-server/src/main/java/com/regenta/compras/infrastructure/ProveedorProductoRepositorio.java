package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.compras.application.ProveedorDeProducto;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.ProveedorProductoId;

public interface ProveedorProductoRepositorio
        extends JpaRepository<ProveedorProducto, ProveedorProductoId> {

    List<ProveedorProducto> findByProveedorId(java.util.UUID proveedorId);

    Optional<ProveedorProducto> findByProveedorIdAndProductoId(java.util.UUID proveedorId,
            java.util.UUID productoId);

    /**
     * Los proveedores que venden un producto, con su costo, el preferido
     * primero (HU-046 criterio 3). La RLS ya acota al negocio.
     */
    @Query("""
            select new com.regenta.compras.application.ProveedorDeProducto(
                p.id, p.razonSocial, pp.codigoProveedor, pp.costoUltimo,
                cast(pp.diasEntrega as integer), pp.cantidadMinima, pp.preferido,
                cast(p.diasCredito as integer))
            from ProveedorProducto pp
            join Proveedor p on p.id = pp.proveedorId
            where pp.productoId = :productoId and p.eliminadoEn is null
            order by pp.preferido desc, p.razonSocial asc
            """)
    List<ProveedorDeProducto> proveedoresDelProducto(@Param("productoId") java.util.UUID productoId);

    /**
     * HU-166 · I3: cada producto tiene a lo sumo un proveedor preferido en el negocio. Antes de
     * marcar uno, se desmarcan los demás.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ProveedorProducto pp set pp.preferido = false
            where pp.negocioId = :negocioId and pp.productoId = :productoId
              and pp.proveedorId <> :proveedorId and pp.preferido = true
            """)
    int quitarPreferidoDeLosDemas(@Param("negocioId") java.util.UUID negocioId,
            @Param("productoId") java.util.UUID productoId,
            @Param("proveedorId") java.util.UUID proveedorId);
}
