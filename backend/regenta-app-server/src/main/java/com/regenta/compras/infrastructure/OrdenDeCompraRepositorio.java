package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.OrdenDeCompra;

public interface OrdenDeCompraRepositorio extends JpaRepository<OrdenDeCompra, UUID> {

    Optional<OrdenDeCompra> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<OrdenDeCompra> findByNegocioIdOrderByFechaEmisionDescCreadoEnDesc(UUID negocioId);

    /** HU-184 · Las órdenes en ciertos estados: por recibir (C2) o cerradas con faltante (C3). */
    List<OrdenDeCompra> findByNegocioIdAndEstadoIn(UUID negocioId,
            java.util.Collection<com.regenta.compras.domain.EstadoOrdenCompra> estados);
}
