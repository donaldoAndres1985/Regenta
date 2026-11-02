package com.regenta.comandas.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.ComandaLineaModificador;

public interface ComandaLineaModificadorRepositorio
        extends JpaRepository<ComandaLineaModificador, UUID> {

    List<ComandaLineaModificador> findByLineaIdIn(Collection<UUID> lineaIds);
}
