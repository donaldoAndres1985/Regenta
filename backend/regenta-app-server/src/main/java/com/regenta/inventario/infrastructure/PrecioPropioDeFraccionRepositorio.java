package com.regenta.inventario.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.PrecioPropioDeFraccion;

/** HU-157 · precios propios de fracción por producto. */
public interface PrecioPropioDeFraccionRepositorio
        extends JpaRepository<PrecioPropioDeFraccion, PrecioPropioDeFraccion.Clave> {

    List<PrecioPropioDeFraccion> findByProductoId(UUID productoId);

    Optional<PrecioPropioDeFraccion> findByProductoIdAndMedida(UUID productoId, String medida);
}
