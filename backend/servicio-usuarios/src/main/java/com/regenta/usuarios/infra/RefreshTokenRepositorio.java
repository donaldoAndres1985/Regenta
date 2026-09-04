package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.RefreshToken;

public interface RefreshTokenRepositorio extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUsuarioIdAndRevocadoEnIsNull(UUID usuarioId);

    List<RefreshToken> findByUsuarioIdAndDispositivoIdAndRevocadoEnIsNull(UUID usuarioId,
            String dispositivoId);
}
