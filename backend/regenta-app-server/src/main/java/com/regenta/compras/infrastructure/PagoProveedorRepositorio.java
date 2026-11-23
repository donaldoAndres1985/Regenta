package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.PagoProveedor;

public interface PagoProveedorRepositorio extends JpaRepository<PagoProveedor, UUID> {

    List<PagoProveedor> findByCuentaIdOrderByFechaAsc(UUID cuentaId);

    /** HU-184 C4 · Los pagos vigentes (sin anular) de un periodo, extremos incluidos. */
    List<PagoProveedor> findByNegocioIdAndAnuladoEnIsNullAndFechaBetween(UUID negocioId,
            java.time.LocalDate desde, java.time.LocalDate hasta);
}
