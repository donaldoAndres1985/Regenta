package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato público que el módulo inventario expone a otros módulos (alertas, menú, ventas, compras).
 */
public interface InventarioApi {

    /** Lotes próximos a vencer dentro de la ventana de días configurada. */
    List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias);

    /** Costo unitario por producto de los insumos solicitados. */
    Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds);

    /** Confirma una reserva de stock previa, convirtiéndola en salida definitiva. */
    void confirmarReserva(String origenTipo, UUID origenId);
}
