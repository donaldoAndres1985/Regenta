package com.regenta.recursos.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.recursos.api.AnticipoRequerido;
import com.regenta.recursos.api.BloqueoEnCalendario;
import com.regenta.recursos.api.CotizacionDeEstadia;
import com.regenta.recursos.api.PenalizacionDeCancelacion;
import com.regenta.recursos.api.RecursoReservable;
import com.regenta.recursos.api.RecursosApi;
import com.regenta.recursos.infrastructure.RepositorioDeBloqueos;
import com.regenta.shared.web.NoEncontradoException;

@Service
@Transactional(readOnly = true)
public class RecursosServicio implements RecursosApi {

    private final GestionDeRecursos recursos;
    private final GestionDeTiposDeRecurso tipos;
    private final RepositorioDeBloqueos bloqueos;
    private final CotizadorDeEstadia cotizador;
    private final GestionDePoliticasDeCancelacion politicas;

    public RecursosServicio(GestionDeRecursos recursos,
                            GestionDeTiposDeRecurso tipos,
                            RepositorioDeBloqueos bloqueos,
                            CotizadorDeEstadia cotizador,
                            GestionDePoliticasDeCancelacion politicas) {
        this.recursos = recursos;
        this.tipos = tipos;
        this.bloqueos = bloqueos;
        this.cotizador = cotizador;
        this.politicas = politicas;
    }

    @Override
    public List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId) {
        List<RecursoDelNegocio> lista = recursos.listar(true);
        Map<UUID, TipoDeRecursoDelNegocio> tiposMap = tipos.listar().stream()
                .collect(Collectors.toMap(TipoDeRecursoDelNegocio::id, t -> t, (a, b) -> a, LinkedHashMap::new));

        return lista.stream()
                .filter(r -> tipoRecursoId == null || tipoRecursoId.equals(r.tipoRecursoId()))
                .map(r -> {
                    TipoDeRecursoDelNegocio tipo = tiposMap.get(r.tipoRecursoId());
                    int bufferAntes = tipo != null ? tipo.bufferAntesMin() : 0;
                    int bufferDespues = tipo != null ? tipo.bufferDespuesMin() : 0;
                    return new RecursoReservable(r.id(), r.tipoRecursoId(), r.codigo(), r.nombre(),
                            r.capacidad(), bufferAntes, bufferDespues);
                })
                .toList();
    }

    @Override
    public List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta) {
        return bloqueos.enPeriodo(desde, hasta).stream()
                .map(b -> b.getRecursoId())
                .distinct()
                .toList();
    }

    @Override
    public List<BloqueoEnCalendario> bloqueosEnPeriodo(UUID negocioId, OffsetDateTime desde, OffsetDateTime hasta) {
        return bloqueos.enPeriodo(desde, hasta).stream()
                .map(b -> new BloqueoEnCalendario(b.getRecursoId(), b.getDesde(), b.getHasta(), b.getMotivo().name()))
                .toList();
    }

    @Override
    public CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada, int noches, int personas) {
        return cotizador.cotizar(new SolicitudDeCotizacion(recursoId, fechaEntrada, noches, personas));
    }

    @Override
    public AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada) {
        try {
            AplicacionDeCancelacion app = politicas.aplicar(new SolicitudDeAplicacionDeCancelacion(politicaCancelacionId, total, entrada));
            return new AnticipoRequerido(app.politicaId(), app.anticipoRequerido());
        } catch (NoEncontradoException e) {
            if (politicaCancelacionId == null) {
                return AnticipoRequerido.ninguno();
            }
            throw e;
        }
    }

    @Override
    public PenalizacionDeCancelacion penalizacionPorCancelar(UUID negocioId, UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada) {
        try {
            AplicacionDeCancelacion app = politicas.aplicar(new SolicitudDeAplicacionDeCancelacion(politicaCancelacionId, total, entrada));
            return new PenalizacionDeCancelacion(app.penalizacion(), app.dentroDePlazo());
        } catch (NoEncontradoException e) {
            if (politicaCancelacionId == null) {
                return PenalizacionDeCancelacion.sinCosto();
            }
            throw e;
        }
    }
}
