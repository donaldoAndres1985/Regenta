package com.regenta.alertas.aplicacion;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.PreferenciaNotificacion;
import com.regenta.alertas.infra.PreferenciaNotificacionRepositorio;
import com.regenta.alertas.infra.TipoAlertaRepositorio;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** Las preferencias de notificación del usuario: canales y "no molestar" (HU-094 criterio 4). */
@Service
public class GestionDePreferencias {

    private final PreferenciaNotificacionRepositorio preferencias;
    private final TipoAlertaRepositorio tipos;

    public GestionDePreferencias(PreferenciaNotificacionRepositorio preferencias,
            TipoAlertaRepositorio tipos) {
        this.preferencias = preferencias;
        this.tipos = tipos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public List<PreferenciaDelUsuario> mias() {
        return preferencias.findByNegocioIdAndUsuarioId(ContextoDeNegocio.negocioActual(),
                        ContextoDeNegocio.usuarioActual())
                .stream().map(PreferenciaDelUsuario::de).toList();
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public PreferenciaDelUsuario fijar(SolicitudDePreferencia solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.usuarioActual();
        String tipoCodigo = solicitud.tipoCodigo().trim();
        if (tipos.findById(tipoCodigo).isEmpty()) {
            throw new ReglaDeNegocioException("Tipo de alerta desconocido: " + tipoCodigo);
        }
        LocalTime desde = solicitud.noMolestarDesde();
        LocalTime hasta = solicitud.noMolestarHasta();
        if ((desde == null) != (hasta == null)) {
            throw new ReglaDeNegocioException(
                    "La franja de no molestar necesita hora de inicio y de fin");
        }

        PreferenciaNotificacion pref = preferencias
                .findByUsuarioIdAndTipoCodigo(usuarioId, tipoCodigo)
                .map(existente -> {
                    existente.aplicar(solicitud.canales(),
                            solicitud.habilitada() == null || solicitud.habilitada(), desde, hasta);
                    return existente;
                })
                .orElseGet(() -> PreferenciaNotificacion.de(negocioId, usuarioId, tipoCodigo,
                        solicitud.canales(),
                        solicitud.habilitada() == null || solicitud.habilitada(), desde, hasta));
        preferencias.save(pref);
        return PreferenciaDelUsuario.de(pref);
    }
}
