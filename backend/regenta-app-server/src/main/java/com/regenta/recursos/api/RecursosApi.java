package com.regenta.recursos.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Contrato público que el módulo recursos expone a otros módulos (como reservas).
 */
public interface RecursosApi {

    /** Recursos activos y reservables del negocio; si tipoRecursoId no es null, solo de ese tipo. */
    List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId);

    /** Ids de recursos con un bloqueo que pisa [desde, hasta). */
    List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta);

    /** Los bloqueos que pisan [desde, hasta), con su tramo y su motivo (HU-075). */
    List<BloqueoEnCalendario> bloqueosEnPeriodo(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta);

    /** Cotiza una estadía noche por noche a su mejor tarifa. */
    CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada, int noches, int personas);

    /** Anticipo exigido por la política de cancelación. */
    AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada);

    /** Penalización aplicable por cancelar una reserva. */
    PenalizacionDeCancelacion penalizacionPorCancelar(UUID negocioId, UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada);
}
