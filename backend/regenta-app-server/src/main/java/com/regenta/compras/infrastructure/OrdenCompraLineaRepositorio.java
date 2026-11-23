package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.compras.domain.OrdenCompraLinea;

public interface OrdenCompraLineaRepositorio extends JpaRepository<OrdenCompraLinea, UUID> {

    List<OrdenCompraLinea> findByOrdenIdOrderByLinea(UUID ordenId);

    /** HU-184 · Las líneas de varias órdenes, para los informes. */
    List<OrdenCompraLinea> findByOrdenIdInOrderByLinea(java.util.Collection<UUID> ordenIds);

    boolean existsByOrdenId(UUID ordenId);

    @Modifying
    @Query("delete from OrdenCompraLinea l where l.ordenId = :ordenId")
    void borrarDeLaOrden(@Param("ordenId") UUID ordenId);
}
