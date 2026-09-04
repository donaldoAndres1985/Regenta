package com.regenta.usuarios.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.usuarios.domain.Usuario;

public interface UsuarioRepositorio extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByNegocioIdAndEmail(UUID negocioId, String email);

    boolean existsByNegocioIdAndEmail(UUID negocioId, String email);

    List<Usuario> findByNegocioIdOrderByNombre(UUID negocioId);

    /** Los que ocupan cupo del plan: activos e invitados sin aceptar todavia. */
    long countByNegocioIdAndEstadoIn(UUID negocioId, List<String> estados);

    /** Cuantos usuarios tienen ese rol: un rol con gente no se borra. */
    @Query("select count(u) from Usuario u join u.roles asignacion where asignacion.rolId = :rolId")
    long contarConRol(@Param("rolId") UUID rolId);
}
