package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.ListaDePrecios;

public interface ListaDePreciosRepositorio extends JpaRepository<ListaDePrecios, UUID> {

    boolean existsByNegocioIdAndNombreIgnoreCase(UUID negocioId, String nombre);

    List<ListaDePrecios> findByNegocioIdOrderByNombre(UUID negocioId);
}
