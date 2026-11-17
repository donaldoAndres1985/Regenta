package com.regenta.inventario.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.TablaDeFracciones;

/** HU-156 · tablas de fracciones del negocio. */
public interface TablaDeFraccionesRepositorio extends JpaRepository<TablaDeFracciones, UUID> {

    List<TablaDeFracciones> findByNegocioIdOrderByNombre(UUID negocioId);

    boolean existsByNegocioIdAndNombreIgnoreCase(UUID negocioId, String nombre);
}
