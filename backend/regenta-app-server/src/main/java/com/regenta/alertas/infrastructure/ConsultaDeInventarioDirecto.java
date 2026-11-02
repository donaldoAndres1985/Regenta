package com.regenta.alertas.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.alertas.application.ConsultaDeInventario;
import com.regenta.inventario.api.InventarioApi;

/**
 * Consulta de lotes por vencer invocando directamente el API pública del módulo inventario.
 */
@Component
public class ConsultaDeInventarioDirecto implements ConsultaDeInventario {

    private final InventarioApi inventarioApi;

    public ConsultaDeInventarioDirecto(InventarioApi inventarioApi) {
        this.inventarioApi = inventarioApi;
    }

    @Override
    public List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias) {
        var lotes = inventarioApi.lotesPorVencer(negocioId, ventanaDias);
        if (lotes == null) {
            return List.of();
        }
        return lotes.stream()
                .map(l -> new LotePorVencer(
                        l.loteId(),
                        l.productoId(),
                        l.productoNombre(),
                        l.codigoLote(),
                        l.diasParaVencer(),
                        l.fechaVencimiento(),
                        l.existencia()))
                .toList();
    }
}
