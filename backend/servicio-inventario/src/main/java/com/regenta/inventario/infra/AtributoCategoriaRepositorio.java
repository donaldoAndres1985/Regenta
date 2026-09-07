package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.AtributoCategoria;

public interface AtributoCategoriaRepositorio extends JpaRepository<AtributoCategoria, UUID> {

    List<AtributoCategoria> findByCategoriaIdOrderByOrdenAscNombreCampoAsc(UUID categoriaId);

    List<AtributoCategoria> findByCategoriaIdAndHeredableTrueOrderByOrdenAsc(UUID categoriaId);

    boolean existsByCategoriaIdAndNombreCampoIgnoreCase(UUID categoriaId, String nombreCampo);
}
