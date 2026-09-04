package com.regenta.usuarios.infra;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Permiso;

/** Catalogo global de permisos. */
public interface PermisoRepositorio extends JpaRepository<Permiso, String> {

    long countByCodigoIn(Collection<String> codigos);

    List<Permiso> findByModuloCodigoInOrderByCodigo(Collection<String> modulos);
}
