package com.regenta.menu.infra;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.regenta.menu.aplicacion.CostosDeInventario;

/**
 * Stub de los costos de servicio-inventario (HU-079). Los tests cargan costos
 * con {@link #cargarCosto}. Reemplaza a {@link CostosDeInventarioHttp} en los
 * tests; en producción los costos se le preguntan a inventario (HU-138).
 */
@Component
@Primary
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
