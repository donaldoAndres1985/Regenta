package com.regenta.reservas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.reservas.aplicacion.DisponibilidadEnPeriodo.RecursoLibre;
import com.regenta.reservas.aplicacion.DisponibilidadEnPeriodo.RecursoOcupado;
import com.regenta.reservas.domain.VentanaOcupada;
import com.regenta.reservas.infra.ConsultaDeOcupacion;

/**
 * "¿Qué recursos tengo libres entre estas dos fechas?" (HU-069). Parte del
 * catálogo de recursos reservables y descuenta: los que tienen un bloqueo en el
 * periodo (criterio 1), y los que tienen una reserva que ocupa —PENDIENTE,
 * CONFIRMADA o CHECK_IN— cuyo tramo, ensanchado por el buffer del tipo, pisa el
 * periodo (criterios 1, 3 y 4). Las canceladas y los no-show no cuentan
 * (criterio 2).
 */
@Service
public class ConsultaDeDisponibilidad {

    private final CatalogoDeRecursos catalogo;
    private final ConsultaDeOcupacion ocupacion;

    public ConsultaDeDisponibilidad(CatalogoDeRecursos catalogo, ConsultaDeOcupacion ocupacion) {
        this.catalogo = catalogo;
        this.ocupacion = ocupacion;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public DisponibilidadEnPeriodo consultar(SolicitudDeDisponibilidad solicitud) {
        OffsetDateTime desde = solicitud.desde();
        OffsetDateTime hasta = solicitud.hasta();
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new ReglaDeNegocioException(
                    "El periodo necesita un inicio y un fin posterior");
        }
        UUID negocioId = ContextoDeNegocio.negocioActual();
        int personas = solicitud.personas() == null ? 0 : solicitud.personas();

        List<RecursoReservable> recursos = catalogo.reservables(negocioId,
                solicitud.tipoRecursoId());
        if (recursos.isEmpty()) {
            return new DisponibilidadEnPeriodo(desde, hasta, 0, List.of(), List.of());
        }

        int maxBuffer = recursos.stream()
                .mapToInt(r -> Math.max(r.bufferAntesMin(), r.bufferDespuesMin()))
                .max().orElse(0);
        Map<UUID, List<VentanaOcupada>> ocupadasPorRecurso = ocupacion.ventanasOcupadas(negocioId,
                        desde.minusMinutes(maxBuffer), hasta.plusMinutes(maxBuffer),
                        solicitud.tipoRecursoId())
                .stream().collect(Collectors.groupingBy(VentanaOcupada::recursoId));
        Set<UUID> bloqueados = Set.copyOf(catalogo.recursosBloqueados(negocioId, desde, hasta));

        List<RecursoLibre> libres = new java.util.ArrayList<>();
        List<RecursoOcupado> ocupados = new java.util.ArrayList<>();

        for (RecursoReservable r : recursos) {
            if (personas > 0 && r.capacidad() < personas) {
                continue;
            }
            if (bloqueados.contains(r.id())) {
                ocupados.add(new RecursoOcupado(r.id(), r.codigo(), "BLOQUEO"));
                continue;
            }
            boolean tomado = ocupadasPorRecurso.getOrDefault(r.id(), List.of()).stream()
                    .anyMatch(v -> v.chocaCon(desde, hasta, r.bufferAntesMin(),
                            r.bufferDespuesMin()));
            if (tomado) {
                ocupados.add(new RecursoOcupado(r.id(), r.codigo(), "RESERVA"));
            } else {
                libres.add(new RecursoLibre(r.id(), r.codigo(), r.nombre(), r.capacidad()));
            }
        }
        return new DisponibilidadEnPeriodo(desde, hasta, recursos.size(), libres, ocupados);
    }
}
