package com.regenta.ventas.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.CotizacionLinea;

public interface CotizacionLineaRepositorio extends JpaRepository<CotizacionLinea, UUID> {

    List<CotizacionLinea> findByCotizacionIdOrderByLinea(UUID cotizacionId);
}
