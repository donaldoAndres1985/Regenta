package com.regenta.recursos.infra;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.recursos.aplicacion.ConsultaDeReservasDeRecurso;

/**
 * Stub de la consulta a servicio-reservas (HU-065 criterio 3). Los tests marcan
 * qué recursos tienen reservas futuras con {@link #conReservasFuturas}; la
 * integración REST real queda como seguimiento.
 */
@Component
public class ConsultaDeReservasDeRecursoStub implements ConsultaDeReservasDeRecurso {

    private final Set<UUID> conReservas = new HashSet<>();

    public void conReservasFuturas(UUID recursoId) {
        conReservas.add(recursoId);
    }

    public void reiniciar() {
        conReservas.clear();
    }

    @Override
    public boolean tieneReservasFuturas(UUID negocioId, UUID recursoId) {
        return conReservas.contains(recursoId);
    }
}
