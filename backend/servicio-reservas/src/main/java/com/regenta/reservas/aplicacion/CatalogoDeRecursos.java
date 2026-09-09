package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que servicio-reservas necesita de servicio-recursos: el catálogo de
 * recursos reservables con el buffer de su tipo y sus bloqueos (HU-069), y la
 * cotización de una estadía y el anticipo requerido por la política de
 * cancelación (HU-070). La implementación real consulta por REST (o mantiene una
 * réplica por eventos); hasta entonces, un stub.
 */
public interface CatalogoDeRecursos {

    /** Recursos activos y reservables del negocio; si {@code tipoRecursoId} no es null, solo de ese tipo. */
    List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId);

    /** Ids de recursos con un bloqueo que pisa {@code [desde, hasta)}. */
    List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta);

    /**
     * Cotiza una estadía noche por noche: cada noche toma la tarifa de mayor
     * prioridad que le aplique (HU-070 criterio 4, delegado en HU-066).
     */
    CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada, int noches,
            int personas);

    /**
     * El anticipo que exige la política de cancelación para una reserva de
     * {@code total} que entra en {@code entrada} (HU-070 criterio 5, delegado en
     * HU-068). Sin {@code politicaCancelacionId} usa la política por defecto del
     * negocio.
     */
    AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total,
            OffsetDateTime entrada);

    /**
     * Lo que retiene la política de cancelación si la reserva se cancela ahora
     * (HU-071 criterios 2 y 3): cero si se cancela con la antelación que pide la
     * política, si no una fracción del total. Sin {@code politicaCancelacionId}
     * usa la política por defecto del negocio.
     */
    PenalizacionDeCancelacion penalizacionPorCancelar(UUID negocioId, UUID politicaCancelacionId,
            BigDecimal total, OffsetDateTime entrada);
}
