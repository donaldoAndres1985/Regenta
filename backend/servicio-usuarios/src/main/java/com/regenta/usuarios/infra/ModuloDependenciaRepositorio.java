package com.regenta.usuarios.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.ClaveDependencia;
import com.regenta.usuarios.domain.ModuloDependencia;

/** Catalogo global: el grafo es el mismo para todos los negocios. */
public interface ModuloDependenciaRepositorio
        extends JpaRepository<ModuloDependencia, ClaveDependencia> {

    List<ModuloDependencia> findByClaveModuloCodigo(String moduloCodigo);

    List<ModuloDependencia> findByClaveDependeDe(String dependeDe);
}
