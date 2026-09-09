package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * El costo unitario de los insumos que servicio-menu necesita para valorar una
 * receta (HU-079 criterios 1 y 2). Lo sabe servicio-inventario; la
 * implementación real consulta por REST (o mantiene una réplica por eventos),
 * hasta entonces un stub.
 */
public interface CostosDeInventario {

    /** Costo unitario por producto. Un producto sin costo conocido no aparece en el mapa. */
    Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds);
}
