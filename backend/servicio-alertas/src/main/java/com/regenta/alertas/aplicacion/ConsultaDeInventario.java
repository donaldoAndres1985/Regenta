package com.regenta.alertas.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lo que el barrido de vencimientos (HU-093 criterio 2) necesita saber del
 * inventario de otro servicio. La implementación real consulta a
 * {@code servicio-inventario} por REST; hasta entonces, un stub la reemplaza.
 */
public interface ConsultaDeInventario {

    List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias);

    record LotePorVencer(
            UUID loteId,
            UUID productoId,
            String productoNombre,
            String codigoLote,
            int diasParaVencer,
            LocalDate fechaVencimiento,
            java.math.BigDecimal existencia) {
    }
}
