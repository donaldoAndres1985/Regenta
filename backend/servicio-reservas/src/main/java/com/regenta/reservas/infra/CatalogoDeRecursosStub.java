package com.regenta.reservas.infra;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.reservas.aplicacion.CatalogoDeRecursos;
import com.regenta.reservas.aplicacion.RecursoReservable;

/**
 * Stub del catálogo de servicio-recursos (HU-069). Los tests cargan recursos con
 * {@link #agregar} y bloqueos con {@link #bloquear}; la integración REST real (o
 * una réplica por eventos) queda como seguimiento.
 */
@Component
public class CatalogoDeRecursosStub implements CatalogoDeRecursos {

    private final Map<UUID, RecursoReservable> recursos = new LinkedHashMap<>();
    private final List<Bloqueo> bloqueos = new ArrayList<>();

    private record Bloqueo(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta) {
        boolean choca(OffsetDateTime d, OffsetDateTime h) {
            return desde.isBefore(h) && hasta.isAfter(d);
        }
    }

    public RecursoReservable agregar(UUID negocioId, UUID tipoRecursoId, String codigo,
            int capacidad, int bufferAntesMin, int bufferDespuesMin) {
        RecursoReservable r = new RecursoReservable(UUID.randomUUID(), tipoRecursoId, codigo,
                "Recurso " + codigo, capacidad, bufferAntesMin, bufferDespuesMin);
        recursos.put(r.id(), r);
        return r;
    }

    public void bloquear(UUID recursoId, OffsetDateTime desde, OffsetDateTime hasta) {
        bloqueos.add(new Bloqueo(recursoId, desde, hasta));
    }

    public void reiniciar() {
        recursos.clear();
        bloqueos.clear();
    }

    @Override
    public List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId) {
        return recursos.values().stream()
                .filter(r -> tipoRecursoId == null || tipoRecursoId.equals(r.tipoRecursoId()))
                .toList();
    }

    @Override
    public List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        return bloqueos.stream().filter(b -> b.choca(desde, hasta)).map(Bloqueo::recursoId)
                .distinct().toList();
    }
}
