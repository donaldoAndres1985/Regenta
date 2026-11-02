package com.regenta.menu.infrastructure;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.inventario.api.InventarioApi;
import com.regenta.menu.application.CostosDeInventario;

/**
 * Adaptador en proceso que consulta los costos unitarios de insumos directamente
 * a través de {@link InventarioApi}, eliminando la llamada HTTP interna.
 */
@Component
public class CostosDeInventarioDirecto implements CostosDeInventario {

    private final InventarioApi inventario;

    public CostosDeInventarioDirecto(InventarioApi inventario) {
        this.inventario = inventario;
    }

    @Override
    public Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds) {
        return inventario.costoUnitarioDe(negocioId, productoIds);
    }
}
