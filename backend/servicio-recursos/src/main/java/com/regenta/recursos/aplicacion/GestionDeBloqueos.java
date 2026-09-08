package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.BloqueoDeRecurso;
import com.regenta.recursos.domain.MotivoBloqueo;
import com.regenta.recursos.infra.RecursoRepositorio;
import com.regenta.recursos.infra.RepositorioDeBloqueos;

/**
 * Bloqueos de recurso por mantenimiento (HU-067). Un bloqueo saca al recurso de
 * disponible en su periodo (criterio 1). Dos bloqueos del mismo recurso no se
 * pueden solapar: lo impide el {@code EXCLUDE USING gist} de la tabla, y aquí se
 * traduce a un 409 (criterio 2). Si el periodo pisa reservas confirmadas, el
 * bloqueo se crea igual pero la respuesta las lista (criterio 3).
 */
@Service
public class GestionDeBloqueos {

    private final RepositorioDeBloqueos bloqueos;
    private final RecursoRepositorio recursos;
    private final ConsultaDeReservasDeRecurso reservas;

    public GestionDeBloqueos(RepositorioDeBloqueos bloqueos, RecursoRepositorio recursos,
            ConsultaDeReservasDeRecurso reservas) {
        this.bloqueos = bloqueos;
        this.recursos = recursos;
        this.reservas = reservas;
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public BloqueoCreado crear(UUID recursoId, SolicitudDeBloqueo solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirRecurso(recursoId, negocioId);

        BloqueoDeRecurso bloqueo = BloqueoDeRecurso.nuevo(negocioId, recursoId, solicitud.desde(),
                solicitud.hasta(), MotivoBloqueo.desde(solicitud.motivo()), solicitud.detalle(),
                ContextoDeNegocio.usuarioActual());

        try {
            bloqueos.insertar(bloqueo);
        } catch (DataIntegrityViolationException choca) {
            throw new ConflictoDeEstadoException(
                    "Ya hay un bloqueo que se solapa con ese periodo en el recurso");
        }

        List<ReservaAfectada> afectadas = reservas.reservasConfirmadasEnPeriodo(
                negocioId, recursoId, bloqueo.getDesde(), bloqueo.getHasta());

        return new BloqueoCreado(
                BloqueoDelNegocio.de(bloqueos.buscar(bloqueo.getId()).orElseThrow()), afectadas);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<BloqueoDelNegocio> listar(UUID recursoId) {
        exigirRecurso(recursoId, ContextoDeNegocio.negocioActual());
        return bloqueos.porRecurso(recursoId).stream().map(BloqueoDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public DisponibilidadDeRecurso disponibilidad(UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        exigirRecurso(recursoId, ContextoDeNegocio.negocioActual());
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new ReglaDeNegocioException(
                    "El periodo consultado necesita un inicio y un fin posterior");
        }
        List<BloqueoDelNegocio> solapan = bloqueos.queSolapan(recursoId, desde, hasta).stream()
                .map(BloqueoDelNegocio::de).toList();
        return new DisponibilidadDeRecurso(recursoId, desde, hasta, solapan.isEmpty(), solapan);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public void eliminar(UUID bloqueoId) {
        BloqueoDeRecurso bloqueo = bloqueos.buscar(bloqueoId)
                .orElseThrow(() -> new NoEncontradoException("Ese bloqueo no existe"));
        // La RLS ya acota la búsqueda al negocio de la transacción.
        bloqueos.eliminar(bloqueo.getId());
    }

    private void exigirRecurso(UUID recursoId, UUID negocioId) {
        if (recursos.findByIdAndNegocioId(recursoId, negocioId).isEmpty()) {
            throw new NoEncontradoException("Ese recurso no existe");
        }
    }
}
