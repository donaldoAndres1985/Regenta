package com.regenta.reservas.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.reservas.application.CalendarioDeOcupacion.BarraDeBloqueo;
import com.regenta.reservas.application.CalendarioDeOcupacion.BarraDeReserva;
import com.regenta.reservas.application.CalendarioDeOcupacion.RecursoDeCalendario;
import com.regenta.reservas.infrastructure.ConsultaDeOcupacion;

/**
 * El calendario de ocupaciÃ³n (HU-075): para una franja de dÃ­as, todos los
 * recursos con sus reservas y sus bloqueos. El cliente lo pinta como una
 * lÃ­nea de tiempo; aquÃ­ solo se juntan las tres fuentes (recursos y bloqueos por
 * el puerto {@link CatalogoDeRecursos}, reservas de la tabla local).
 */
@Service
public class ConsultaDeCalendario {

    /** Tope de dÃ­as por consulta: un calendario no pide un aÃ±o entero de una. */
    private static final int MAX_DIAS = 62;

    private final CatalogoDeRecursos catalogo;
    private final ConsultaDeOcupacion ocupacion;

    public ConsultaDeCalendario(CatalogoDeRecursos catalogo, ConsultaDeOcupacion ocupacion) {
        this.catalogo = catalogo;
        this.ocupacion = ocupacion;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public CalendarioDeOcupacion calendario(LocalDate desde, LocalDate hasta, UUID tipoRecursoId) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El calendario necesita un desde y un hasta posterior");
        }
        if (desde.plusDays(MAX_DIAS).isBefore(hasta)) {
            throw new ReglaDeNegocioException("El calendario se consulta de a lo sumo " + MAX_DIAS
                    + " dÃ­as");
        }
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin = hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<RecursoDeCalendario> recursos = catalogo.reservables(negocioId, tipoRecursoId).stream()
                .map(r -> new RecursoDeCalendario(r.id(), r.codigo(), r.nombre(), r.capacidad()))
                .toList();
        Set<UUID> idsRecurso = recursos.stream().map(RecursoDeCalendario::id)
                .collect(Collectors.toSet());

        List<BarraDeReserva> reservas = ocupacion
                .reservasEnCalendario(negocioId, inicio, fin, tipoRecursoId).stream()
                .filter(b -> idsRecurso.contains(b.recursoId()))
                .map(b -> new BarraDeReserva(b.id(), b.recursoId(), b.numero(), b.estado(),
                        b.desde(), b.hasta()))
                .toList();

        List<BarraDeBloqueo> bloqueos = catalogo.bloqueosEnPeriodo(negocioId, inicio, fin).stream()
                .filter(b -> idsRecurso.contains(b.recursoId()))
                .map(b -> new BarraDeBloqueo(b.recursoId(), b.desde(), b.hasta(), b.motivo()))
                .toList();

        List<LocalDate> dias = Stream.iterate(desde, d -> !d.isAfter(hasta), d -> d.plusDays(1))
                .toList();

        return new CalendarioDeOcupacion(desde, hasta, dias, recursos, reservas, bloqueos);
    }
}

