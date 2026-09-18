package com.regenta.ventas.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.ventas.domain.Venta;

public interface VentaRepositorio extends JpaRepository<Venta, UUID> {

    /** HU-043 criterio 3: la clave con la que una subida repetida se reconoce. */
    Optional<Venta> findByNegocioIdAndOrigenOfflineId(UUID negocioId, UUID origenOfflineId);
}
