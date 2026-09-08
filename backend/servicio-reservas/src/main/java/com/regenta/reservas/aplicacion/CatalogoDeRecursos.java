package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que servicio-reservas necesita de servicio-recursos para responder qué hay
 * libre (HU-069): el catálogo de recursos reservables con el buffer de su tipo,
 * y qué recursos están bloqueados en un periodo. La implementación real consulta
 * por REST (o mantiene una réplica por eventos); hasta entonces, un stub.
 */
public interface CatalogoDeRecursos {

    /** Recursos activos y reservables del negocio; si {@code tipoRecursoId} no es null, solo de ese tipo. */
    List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId);

    /** Ids de recursos con un bloqueo que pisa {@code [desde, hasta)}. */
    List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta);
}
