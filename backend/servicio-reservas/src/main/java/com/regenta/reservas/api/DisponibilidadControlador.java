package com.regenta.reservas.api;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.reservas.aplicacion.CalendarioDeOcupacion;
import com.regenta.reservas.aplicacion.ConsultaDeCalendario;
import com.regenta.reservas.aplicacion.ConsultaDeDisponibilidad;
import com.regenta.reservas.aplicacion.DisponibilidadEnPeriodo;
import com.regenta.reservas.aplicacion.ReservaDeUnRecurso;
import com.regenta.reservas.aplicacion.ReservasDeUnRecurso;
import com.regenta.reservas.aplicacion.SolicitudDeDisponibilidad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Consulta de disponibilidad de recursos en un periodo. HU-069. */
@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Disponibilidad", description = "Qué recursos hay libres entre dos fechas")
public class DisponibilidadControlador {

    private final ConsultaDeDisponibilidad disponibilidad;
    private final ConsultaDeCalendario calendario;
    private final ReservasDeUnRecurso deUnRecurso;

    public DisponibilidadControlador(ConsultaDeDisponibilidad disponibilidad,
            ConsultaDeCalendario calendario, ReservasDeUnRecurso deUnRecurso) {
        this.disponibilidad = disponibilidad;
        this.calendario = calendario;
        this.deUnRecurso = deUnRecurso;
    }

    @GetMapping("/disponibilidad")
    @Operation(summary = "Recursos libres entre desde y hasta, descontando reservas y bloqueos")
    public DisponibilidadEnPeriodo consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) UUID tipoRecursoId,
            @RequestParam(required = false) Integer personas) {
        return disponibilidad.consultar(
                new SolicitudDeDisponibilidad(desde, hasta, tipoRecursoId, personas));
    }

    @GetMapping("/calendario")
    @Operation(summary = "Ocupación de todos los recursos día a día: reservas como barras y bloqueos")
    public CalendarioDeOcupacion calendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID tipoRecursoId) {
        return calendario.calendario(desde, hasta, tipoRecursoId);
    }

    @GetMapping("/de-recurso/{recursoId}")
    @Operation(summary = "Las reservas de un recurso en un periodo; sin hasta, de desde en adelante "
            + "(lo consume servicio-recursos, HU-138)")
    public List<ReservaDeUnRecurso> deUnRecurso(@PathVariable UUID recursoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "false") boolean soloConfirmadas) {
        return deUnRecurso.delRecurso(recursoId, desde, hasta, soloConfirmadas);
    }
}
