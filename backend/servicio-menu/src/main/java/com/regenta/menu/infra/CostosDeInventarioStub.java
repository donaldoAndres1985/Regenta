package com.regenta.menu.infra;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.menu.aplicacion.CostosDeInventario;

/**
 * Stub de los costos de servicio-inventario (HU-079). Los tests cargan costos
 * con {@link #cargarCosto}; la integración real (REST o réplica por eventos)
 * queda como seguimiento.
 */
@Component
public class CostosDeInventarioStub implements CostosDeInventario {

    private final Map<UUID, BigDecimal> costos = new LinkedHashMap<>();

    public void cargarCosto(UUID productoId, BigDecimal costoUnitario) {
        costos.put(productoId, costoUnitario);
    }

    public void reiniciar() {
        costos.clear();
    }

    @Override
    public Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds) {
        Map<UUID, BigDecimal> out = new LinkedHashMap<>();
        for (UUID id : productoIds) {
            BigDecimal c = costos.get(id);
            if (c != null) {
                out.put(id, c);
            }
        }
        return out;
    }
}
