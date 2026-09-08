package com.regenta.recursos.infra;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.recursos.aplicacion.ConsultaDeReservasDeRecurso;
import com.regenta.recursos.aplicacion.ReservaAfectada;

/**
 * Stub de la consulta a servicio-reservas (HU-065 y HU-067). Los tests marcan
 * qué recursos tienen reservas futuras con {@link #conReservasFuturas} y qué
 * reservas confirmadas hay en el calendario con {@link #conReservaConfirmada};
 * la integración REST real queda como seguimiento.
 */
@Component
public class ConsultaDeReservasDeRecursoStub implements ConsultaDeReservasDeRecurso {

    private final Set<UUID> conReservas = new HashSet<>();
    private final Map<UUID, List<ReservaAfectada>> confirmadasPorRecurso = new HashMap<>();

    public void conReservasFuturas(UUID recursoId) {
        conReservas.add(recursoId);
    }

    public void conReservaConfirmada(UUID recursoId, ReservaAfectada reserva) {
        confirmadasPorRecurso.computeIfAbsent(recursoId, r -> new ArrayList<>()).add(reserva);
    }

    public void reiniciar() {
        conReservas.clear();
        confirmadasPorRecurso.clear();
    }

    @Override
    public boolean tieneReservasFuturas(UUID negocioId, UUID recursoId) {
        return conReservas.contains(recursoId);
    }

    @Override
    public List<ReservaAfectada> reservasConfirmadasEnPeriodo(UUID negocioId, UUID recursoId,
            OffsetDateTime desde, OffsetDateTime hasta) {
        return confirmadasPorRecurso.getOrDefault(recursoId, List.of()).stream()
                .filter(r -> r.entrada().isBefore(hasta) && r.salida().isAfter(desde))
                .toList();
    }
}
