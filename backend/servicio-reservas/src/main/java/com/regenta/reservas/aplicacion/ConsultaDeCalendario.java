package com.regenta.reservas.aplicacion;

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

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.reservas.aplicacion.CalendarioDeOcupacion.BarraDeBloqueo;
import com.regenta.reservas.aplicacion.CalendarioDeOcupacion.BarraDeReserva;
import com.regenta.reservas.aplicacion.CalendarioDeOcupacion.RecursoDeCalendario;
import com.regenta.reservas.infra.ConsultaDeOcupacion;

/**
 * El calendario de ocupación (HU-075): para una franja de días, todos los
 * recursos con sus reservas y sus bloqueos. El cliente lo pinta como una
 * línea de tiempo; aquí solo se juntan las tres fuentes (recursos y bloqueos por
 * el puerto {@link CatalogoDeRecursos}, reservas de la tabla local).
 */
@Service
public class ConsultaDeCalendario {

    /** Tope de días por consulta: un calendario no pide un año entero de una. */
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
                    + " días");
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
