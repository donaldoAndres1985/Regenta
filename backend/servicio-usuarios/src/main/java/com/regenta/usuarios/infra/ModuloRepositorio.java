package com.regenta.usuarios.infra;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Modulo;

/** Catalogo global de modulos. */
public interface ModuloRepositorio extends JpaRepository<Modulo, String> {

    List<Modulo> findByCodigoInOrderByOrden(Collection<String> codigos);

    List<Modulo> findAllByOrderByOrden();
}
