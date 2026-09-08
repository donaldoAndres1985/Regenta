package com.regenta.alertas.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.Alerta;
import com.regenta.alertas.infra.AlertaRepositorio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** El centro de alertas del usuario (HU-094 criterio 2, base de HU-095). */
@Service
public class ConsultaDeAlertas {

    private final AlertaRepositorio alertas;

    public ConsultaDeAlertas(AlertaRepositorio alertas) {
        this.alertas = alertas;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public List<AlertaDelUsuario> mias() {
        return alertas.delUsuario(ContextoDeNegocio.negocioActual(),
                        ContextoDeNegocio.usuarioActual())
                .stream().map(AlertaDelUsuario::de).toList();
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
            OffsetDateTime generadaEn) {

        static AlertaDelUsuario de(Alerta a) {
            return new AlertaDelUsuario(a.getId(), a.getTipoCodigo(), a.getSeveridad().name(),
                    a.getEstado().name(), a.getTitulo(), a.getMensaje(), a.getRutaApp(),
                    a.getEntidadTipo(), a.getEntidadId(), a.getGeneradaEn());
        }
    }
}
