package com.regenta.ventas.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.Cotizacion;

public interface CotizacionRepositorio extends JpaRepository<Cotizacion, UUID> {

    List<Cotizacion> findByNegocioIdOrderByCreadoEnDesc(UUID negocioId);
}
