package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByNegocioIdAndEmail(UUID negocioId, String email);

    boolean existsByNegocioIdAndEmail(UUID negocioId, String email);

    List<Usuario> findByNegocioIdOrderByNombre(UUID negocioId);

    /** Los que ocupan cupo del plan: activos e invitados sin aceptar todavia. */
    long countByNegocioIdAndEstadoIn(UUID negocioId, List<String> estados);
}
