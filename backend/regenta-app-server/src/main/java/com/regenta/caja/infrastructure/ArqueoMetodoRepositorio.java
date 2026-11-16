package com.regenta.caja.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.caja.domain.ArqueoMetodo;

/** HU-140 · el arqueo por método de pago guardado al cerrar cada sesión. */
public interface ArqueoMetodoRepositorio extends JpaRepository<ArqueoMetodo, UUID> {

    List<ArqueoMetodo> findBySesionIdOrderByMetodoPago(UUID sesionId);
}
