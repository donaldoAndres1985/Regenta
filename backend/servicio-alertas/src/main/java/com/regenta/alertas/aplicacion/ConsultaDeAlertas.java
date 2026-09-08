package com.regenta.alertas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.Alerta;
import com.regenta.alertas.domain.EstadoAlerta;
import com.regenta.alertas.infra.AlertaRepositorio;
import com.regenta.alertas.infra.EntregaRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * El centro de alertas del usuario (HU-095): sus alertas, marcarlas vistas al
 * abrir y resolverlas cuando corresponde.
 */
@Service
public class ConsultaDeAlertas {

    private final AlertaRepositorio alertas;
    private final EntregaRepositorio entregas;

    public ConsultaDeAlertas(AlertaRepositorio alertas, EntregaRepositorio entregas) {
        this.alertas = alertas;
        this.entregas = entregas;
    }

    /** Criterio 1: las nuevas primero, luego por severidad, luego por fecha. */
    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public List<AlertaDelUsuario> mias() {
        return alertas.delUsuario(ContextoDeNegocio.negocioActual(),
                        ContextoDeNegocio.usuarioActual())
                .stream().map(AlertaDelUsuario::de).toList();
    }

    /** Criterio 4: al abrir el centro, las nuevas del usuario pasan a vistas. */
    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public int marcarMiasVistas() {
        List<Alerta> nuevas = alertas
                .delUsuario(ContextoDeNegocio.negocioActual(), ContextoDeNegocio.usuarioActual())
                .stream().filter(a -> a.getEstado() == EstadoAlerta.NUEVA).toList();
        for (Alerta a : nuevas) {
            a.marcarVista();
            alertas.save(a);
        }
        return nuevas.size();
    }

    /** Criterio 2: resolver deja quién y cuándo, y sale de las pendientes. */
    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public AlertaDelUsuario resolver(UUID alertaId) {
        UUID usuarioId = ContextoDeNegocio.usuarioActual();
        Alerta alerta = alertas
                .findByIdAndNegocioId(alertaId, ContextoDeNegocio.negocioActual())
                .filter(a -> !entregas.findByAlertaIdAndUsuarioId(alertaId, usuarioId).isEmpty())
                .orElseThrow(() -> new NoEncontradoException("Esa alerta no existe"));
        alerta.resolver(usuarioId);
        alertas.save(alerta);
        return AlertaDelUsuario.de(alerta);
    }

    public record AlertaDelUsuario(
            UUID id,
            String tipoCodigo,
            String severidad,
            String estado,
            String titulo,
            String mensaje,
            String rutaApp,
            String entidadTipo,
            UUID entidadId,
            OffsetDateTime generadaEn,
            UUID resueltaPor) {

        static AlertaDelUsuario de(Alerta a) {
            return new AlertaDelUsuario(a.getId(), a.getTipoCodigo(), a.getSeveridad().name(),
                    a.getEstado().name(), a.getTitulo(), a.getMensaje(), a.getRutaApp(),
                    a.getEntidadTipo(), a.getEntidadId(), a.getGeneradaEn(), a.getResueltaPor());
        }
    }
}
