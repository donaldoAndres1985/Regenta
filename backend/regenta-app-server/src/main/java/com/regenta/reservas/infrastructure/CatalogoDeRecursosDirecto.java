package com.regenta.reservas.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.recursos.api.RecursosApi;
import com.regenta.reservas.application.AnticipoRequerido;
import com.regenta.reservas.application.BloqueoEnCalendario;
import com.regenta.reservas.application.CatalogoDeRecursos;
import com.regenta.reservas.application.CotizacionDeEstadia;
import com.regenta.reservas.application.CotizacionDeEstadia.NocheCotizada;
import com.regenta.reservas.application.PenalizacionDeCancelacion;
import com.regenta.reservas.application.RecursoReservable;

/**
 * Adaptador directo hacia el módulo recursos a través de RecursosApi.
 */
@Component
public class CatalogoDeRecursosDirecto implements CatalogoDeRecursos {

    private final RecursosApi recursosApi;

    public CatalogoDeRecursosDirecto(RecursosApi recursosApi) {
        this.recursosApi = recursosApi;
    }

    @Override
    public List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId) {
        return recursosApi.reservables(negocioId, tipoRecursoId).stream()
                .map(r -> new RecursoReservable(
                        r.id(),
                        r.tipoRecursoId(),
                        r.codigo(),
                        r.nombre(),
                        r.capacidad(),
                        r.bufferAntesMin(),
                        r.bufferDespuesMin()))
                .toList();
    }

    @Override
    public List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta) {
        return recursosApi.recursosBloqueados(negocioId, desde, hasta);
    }

    @Override
    public List<BloqueoEnCalendario> bloqueosEnPeriodo(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta) {
        return recursosApi.bloqueosEnPeriodo(negocioId, desde, hasta).stream()
                .map(b -> new BloqueoEnCalendario(b.recursoId(), b.desde(), b.hasta(), b.motivo()))
                .toList();
    }

    @Override
    public CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada, int noches, int personas) {
        com.regenta.recursos.api.CotizacionDeEstadia cot = recursosApi.cotizar(negocioId, recursoId, fechaEntrada, noches, personas);
        List<NocheCotizada> nochesCotizadas = cot.noches() == null ? List.of() : cot.noches().stream()
                .map(n -> new NocheCotizada(n.fecha(), n.tarifaId(), n.precio()))
                .toList();
        return new CotizacionDeEstadia(cot.total(), cot.moneda(), cot.completa(), nochesCotizadas);
    }

    @Override
    public AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada) {
        com.regenta.recursos.api.AnticipoRequerido ant = recursosApi.anticipo(negocioId, politicaCancelacionId, total, entrada);
        return new AnticipoRequerido(ant.politicaCancelacionId(), ant.anticipo());
    }

    @Override
    public PenalizacionDeCancelacion penalizacionPorCancelar(UUID negocioId, UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada) {
        com.regenta.recursos.api.PenalizacionDeCancelacion pen = recursosApi.penalizacionPorCancelar(negocioId, politicaCancelacionId, total, entrada);
        return new PenalizacionDeCancelacion(pen.penalizacion(), pen.dentroDePlazo());
    }
}
