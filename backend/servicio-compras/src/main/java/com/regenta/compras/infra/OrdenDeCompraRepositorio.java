package com.regenta.compras.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.OrdenDeCompra;

public interface OrdenDeCompraRepositorio extends JpaRepository<OrdenDeCompra, UUID> {

    Optional<OrdenDeCompra> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<OrdenDeCompra> findByNegocioIdOrderByFechaEmisionDescCreadoEnDesc(UUID negocioId);
}
