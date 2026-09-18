package com.regenta.auditoria.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.auditoria.domain.Dispositivo;

public interface DispositivoRepositorio extends JpaRepository<Dispositivo, UUID> {

    Optional<Dispositivo> findByNegocioIdAndIdentificador(UUID negocioId, String identificador);
}
