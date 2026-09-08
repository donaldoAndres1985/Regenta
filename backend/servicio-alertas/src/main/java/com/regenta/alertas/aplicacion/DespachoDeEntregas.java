package com.regenta.alertas.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.Alerta;
import com.regenta.alertas.domain.CanalDeAlerta;
import com.regenta.alertas.domain.DispositivoPush;
import com.regenta.alertas.domain.Entrega;
import com.regenta.alertas.domain.PreferenciaNotificacion;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.infra.AlertaRepositorio;
import com.regenta.alertas.infra.DispositivoPushRepositorio;
import com.regenta.alertas.infra.EntregaRepositorio;
import com.regenta.alertas.infra.PreferenciaNotificacionRepositorio;
import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * Envía las entregas de una alerta por su canal (HU-094).
 *
 * <ul>
 *   <li>Push a los dispositivos ANDROID/IOS activos del usuario (criterio 1); un
 *       token que FCM rechaza deja el dispositivo inactivo (criterio 5).</li>
 *   <li>In-app: la alerta ya está en la base, la entrega queda {@code ENTREGADA}
 *       y el centro de la app la muestra (criterio 2).</li>
 *   <li>Un envío que falla se reintenta con backoff y guarda el error
 *       (criterio 3).</li>
 *   <li>Si la hora cae en "no molestar" del usuario, se retiene hasta que
 *       termine la franja, salvo severidad CRÍTICA (criterio 4).</li>
 * </ul>
 */
@Service
public class DespachoDeEntregas {

    private final EntregaRepositorio entregas;
    private final AlertaRepositorio alertas;
    private final DispositivoPushRepositorio dispositivos;
    private final PreferenciaNotificacionRepositorio preferencias;
    private final PasarelaDePush push;
    private final PasarelaDeCorreo correo;

    public DespachoDeEntregas(EntregaRepositorio entregas, AlertaRepositorio alertas,
            DispositivoPushRepositorio dispositivos,
            PreferenciaNotificacionRepositorio preferencias, PasarelaDePush push,
            PasarelaDeCorreo correo) {
        this.entregas = entregas;
        this.alertas = alertas;
        this.dispositivos = dispositivos;
        this.preferencias = preferencias;
        this.push = push;
        this.correo = correo;
    }

    @Transactional
    public void despacharAlerta(UUID alertaId) {
        Alerta alerta = alertas.findById(alertaId).orElse(null);
        if (alerta == null) {
            return;
        }
        for (Entrega e : entregas.findByAlertaId(alertaId)) {
            if (e.estaPendienteDeEnvio()) {
                intentar(e, alerta);
            }
        }
    }

    /** Criterio 3: reprocesa la cola de reintentos del negocio. Devuelve cuántas tocó. */
    @Transactional
    public int reintentarPendientes() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Entrega> cola = entregas.paraReintentar(negocioId, OffsetDateTime.now());
        for (Entrega e : cola) {
            Alerta alerta = alertas.findById(e.getAlertaId()).orElse(null);
            if (alerta != null) {
                intentar(e, alerta);
            }
        }
        return cola.size();
    }

    private void intentar(Entrega e, Alerta alerta) {
        PreferenciaNotificacion pref = preferencias
                .findByUsuarioIdAndTipoCodigo(e.getUsuarioId(), alerta.getTipoCodigo())
                .orElse(null);

        if (pref != null && !pref.aceptaCanal(e.getCanal().name())) {
            e.descartarPorPreferencia();
            entregas.save(e);
            return;
        }
        if (pref != null && alerta.getSeveridad() != Severidad.CRITICA) {
            LocalTime ahora = OffsetDateTime.now(ZoneOffset.UTC).toLocalTime();
            if (pref.enNoMolestar(ahora)) {
                e.retenerHasta(finDeVentana(pref.getNoMolestarHasta()));
                entregas.save(e);
                return;
            }
        }

        switch (e.getCanal()) {
            case IN_APP -> e.marcarEntregada();
            case PUSH -> enviarPush(e, alerta);
            case EMAIL -> enviarCorreo(e, alerta);
            default -> e.marcarFallida("Canal no soportado: " + e.getCanal());
        }
        entregas.save(e);
    }

    private void enviarPush(Entrega e, Alerta alerta) {
        List<DispositivoPush> activos = dispositivos
                .findByNegocioIdAndUsuarioIdAndActivoTrue(alerta.getNegocioId(), e.getUsuarioId())
                .stream().filter(DispositivoPush::recibePush).toList();
        if (activos.isEmpty()) {
            e.marcarFallida("El usuario no tiene un dispositivo push activo");
            return;
        }
        Map<String, String> datos = Map.of(
                "alerta_id", alerta.getId().toString(),
                "ruta", alerta.getRutaApp() == null ? "" : alerta.getRutaApp());
        String proveedorId = null;
        String ultimoError = null;
        boolean algunoOk = false;
        for (DispositivoPush d : activos) {
            PasarelaDePush.ResultadoDePush r = push.enviar(d.getTokenFcm(), alerta.getTitulo(),
                    alerta.getMensaje(), datos);
            if (r.tokenInvalido()) {
                d.desactivar();
                dispositivos.save(d);
                ultimoError = r.error();
            } else if (r.exito()) {
                algunoOk = true;
                proveedorId = r.proveedorId();
            } else {
                ultimoError = r.error();
            }
        }
        if (algunoOk) {
            e.marcarEnviada(proveedorId);
        } else {
            e.marcarFallida(ultimoError == null ? "Fallo el envío push" : ultimoError);
        }
    }

    private void enviarCorreo(Entrega e, Alerta alerta) {
        if (e.getDestino() == null || e.getDestino().isBlank()) {
            e.marcarFallida("No hay correo del usuario para la entrega");
            return;
        }
        PasarelaDeCorreo.ResultadoDeCorreo r = correo.enviar(e.getDestino(), alerta.getTitulo(),
                alerta.getMensaje());
        if (r.exito()) {
            e.marcarEnviada(r.proveedorId());
        } else {
            e.marcarFallida(r.error() == null ? "Fallo el envío de correo" : r.error());
        }
    }

    private static OffsetDateTime finDeVentana(LocalTime hasta) {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        LocalDate dia = ahora.toLocalTime().isBefore(hasta)
                ? ahora.toLocalDate() : ahora.toLocalDate().plusDays(1);
        return OffsetDateTime.of(dia, hasta, ZoneOffset.UTC);
    }
}
