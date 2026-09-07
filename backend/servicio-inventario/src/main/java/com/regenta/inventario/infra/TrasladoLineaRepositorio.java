package com.regenta.inventario.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.TrasladoLinea;

public interface TrasladoLineaRepositorio extends JpaRepository<TrasladoLinea, UUID> {

    List<TrasladoLinea> findByTrasladoIdOrderById(UUID trasladoId);
}
