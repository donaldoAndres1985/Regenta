package com.regenta.alertas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.alertas.domain.PreferenciaNotificacion;
import com.regenta.alertas.domain.PreferenciaNotificacionId;

public interface PreferenciaNotificacionRepositorio
        extends JpaRepository<PreferenciaNotificacion, PreferenciaNotificacionId> {

    Optional<PreferenciaNotificacion> findByUsuarioIdAndTipoCodigo(UUID usuarioId,
            String tipoCodigo);

    List<PreferenciaNotificacion> findByNegocioIdAndUsuarioId(UUID negocioId, UUID usuarioId);
}
