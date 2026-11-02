package com.regenta.reservas.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lo que servicio-reservas necesita de servicio-recursos: el catÃ¡logo de
 * recursos reservables con el buffer de su tipo y sus bloqueos (HU-069), y la
 * cotizaciÃ³n de una estadÃ­a y el anticipo requerido por la polÃ­tica de
 * cancelaciÃ³n (HU-070). La implementaciÃ³n real consulta por REST (o mantiene una
 * rÃ©plica por eventos); hasta entonces, un stub.
 */
public interface CatalogoDeRecursos {

    /** Recursos activos y reservables del negocio; si {@code tipoRecursoId} no es null, solo de ese tipo. */
    List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId);

    /** Ids de recursos con un bloqueo que pisa {@code [desde, hasta)}. */
    List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta);

    /** Los bloqueos que pisan {@code [desde, hasta)}, con su tramo y su motivo (HU-075). */
    List<BloqueoEnCalendario> bloqueosEnPeriodo(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta);

    /**
     * Cotiza una estadÃ­a noche por noche: cada noche toma la tarifa de mayor
     * prioridad que le aplique (HU-070 criterio 4, delegado en HU-066).
     */
    CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada, int noches,
            int personas);

    /**
     * El anticipo que exige la polÃ­tica de cancelaciÃ³n para una reserva de
     * {@code total} que entra en {@code entrada} (HU-070 criterio 5, delegado en
     * HU-068). Sin {@code politicaCancelacionId} usa la polÃ­tica por defecto del
     * negocio.
     */
    AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total,
            OffsetDateTime entrada);

    /**
     * Lo que retiene la polÃ­tica de cancelaciÃ³n si la reserva se cancela ahora
     * (HU-071 criterios 2 y 3): cero si se cancela con la antelaciÃ³n que pide la
     * polÃ­tica, si no una fracciÃ³n del total. Sin {@code politicaCancelacionId}
     * usa la polÃ­tica por defecto del negocio.
     */
    PenalizacionDeCancelacion penalizacionPorCancelar(UUID negocioId, UUID politicaCancelacionId,
            BigDecimal total, OffsetDateTime entrada);
}

