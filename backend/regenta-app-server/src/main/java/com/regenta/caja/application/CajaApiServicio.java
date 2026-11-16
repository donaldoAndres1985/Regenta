package com.regenta.caja.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.api.CajaApi;
import com.regenta.caja.domain.EstadoSesionCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;

/** La implementación de {@link CajaApi} que usan los otros módulos. */
@Service
public class CajaApiServicio implements CajaApi {

    private final SesionDeCajaRepositorio sesiones;

    public CajaApiServicio(SesionDeCajaRepositorio sesiones) {
        this.sesiones = sesiones;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> sesionAbiertaDe(UUID negocioId, UUID usuarioId) {
        if (negocioId == null || usuarioId == null) {
            return Optional.empty();
        }
        return sesiones.findFirstByNegocioIdAndUsuarioAperturaIdAndEstadoOrderByAbiertaEnDesc(
                negocioId, usuarioId, EstadoSesionCaja.ABIERTA).map(SesionDeCaja::getId);
    }
}
