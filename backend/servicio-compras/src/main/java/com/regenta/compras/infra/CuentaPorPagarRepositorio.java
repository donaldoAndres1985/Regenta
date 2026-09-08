package com.regenta.compras.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.CuentaPorPagar;

public interface CuentaPorPagarRepositorio extends JpaRepository<CuentaPorPagar, UUID> {

    List<CuentaPorPagar> findByNegocioIdAndRecepcionId(UUID negocioId, UUID recepcionId);
}
