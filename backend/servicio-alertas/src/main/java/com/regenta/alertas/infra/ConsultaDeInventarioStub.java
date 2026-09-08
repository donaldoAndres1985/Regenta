package com.regenta.alertas.infra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.regenta.alertas.aplicacion.ConsultaDeInventario;

/**
 * Stub de la consulta al inventario (HU-093 criterio 2). Devuelve lo que se le
 * cargue con {@link #cargar}; la integración real con {@code servicio-inventario}
 * queda como seguimiento.
 */
@Component
public class ConsultaDeInventarioStub implements ConsultaDeInventario {

    private final Map<UUID, List<LotePorVencer>> porNegocio = new ConcurrentHashMap<>();

    public void cargar(UUID negocioId, List<LotePorVencer> lotes) {
        porNegocio.put(negocioId, List.copyOf(lotes));
    }

    public void reiniciar() {
        porNegocio.clear();
    }

    @Override
    public List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias) {
        List<LotePorVencer> todos = porNegocio.getOrDefault(negocioId, List.of());
        List<LotePorVencer> dentro = new ArrayList<>();
        for (LotePorVencer l : todos) {
            if (l.diasParaVencer() <= ventanaDias) {
                dentro.add(l);
            }
        }
        return dentro;
    }
}
