package com.regenta.inventario.infra;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.inventario.domain.Lote;

public interface LoteRepositorio extends JpaRepository<Lote, UUID> {

    Optional<Lote> findByNegocioIdAndProductoIdAndCodigoLote(
            UUID negocioId, UUID productoId, String codigoLote);
}
