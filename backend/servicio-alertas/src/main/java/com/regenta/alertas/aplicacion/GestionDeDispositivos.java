package com.regenta.alertas.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.DispositivoPush;
import com.regenta.alertas.domain.PlataformaDispositivo;
import com.regenta.alertas.infra.DispositivoPushRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** Registro de los tokens FCM del usuario para recibir push (HU-094 criterios 1 y 5). */
@Service
public class GestionDeDispositivos {

    private final DispositivoPushRepositorio dispositivos;

    public GestionDeDispositivos(DispositivoPushRepositorio dispositivos) {
        this.dispositivos = dispositivos;
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public DispositivoDelUsuario registrar(SolicitudDeDispositivo solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.usuarioActual();
        String token = solicitud.tokenFcm() == null ? "" : solicitud.tokenFcm().trim();
        if (token.isEmpty()) {
            throw new ReglaDeNegocioException("El token FCM es obligatorio");
        }
        PlataformaDispositivo plataforma = plataformaDe(solicitud.plataforma());

        DispositivoPush dispositivo = dispositivos.findByTokenFcm(token)
                .map(existente -> {
                    existente.reasignar(usuarioId, plataforma, solicitud.modelo(),
                            solicitud.versionApp());
                    return existente;
                })
                .orElseGet(() -> DispositivoPush.registrar(negocioId, usuarioId, token, plataforma,
                        solicitud.modelo(), solicitud.versionApp()));
        dispositivos.save(dispositivo);
        return DispositivoDelUsuario.de(dispositivo);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public List<DispositivoDelUsuario> mios() {
        return dispositivos.findByNegocioIdAndUsuarioIdAndActivoTrue(
                        ContextoDeNegocio.negocioActual(), ContextoDeNegocio.usuarioActual())
                .stream().map(DispositivoDelUsuario::de).toList();
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public void eliminar(UUID dispositivoId) {
        DispositivoPush d = dispositivos
                .findByIdAndNegocioId(dispositivoId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese dispositivo no existe"));
        d.desactivar();
        dispositivos.save(d);
    }

    private static PlataformaDispositivo plataformaDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return PlataformaDispositivo.ANDROID;
        }
        try {
            return PlataformaDispositivo.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Plataforma no válida: " + texto);
        }
    }
}
