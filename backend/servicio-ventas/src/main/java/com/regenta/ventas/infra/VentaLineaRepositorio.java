package com.regenta.ventas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.VentaLinea;

public interface VentaLineaRepositorio extends JpaRepository<VentaLinea, UUID> {

    List<VentaLinea> findByVentaIdOrderByLinea(UUID ventaId);

    Optional<VentaLinea> findByVentaIdAndLinea(UUID ventaId, short linea);

    long countByVentaId(UUID ventaId);
}
