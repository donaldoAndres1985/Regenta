package com.regenta.facturacion.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.facturacion.domain.Contingencia;

public interface ContingenciaRepositorio extends JpaRepository<Contingencia, UUID> {

    Optional<Contingencia> findByNegocioIdAndFinEnIsNull(UUID negocioId);

    Optional<Contingencia> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<Contingencia> findByNegocioIdOrderByInicioEnDesc(UUID negocioId);

    /** Suma una factura afectada, atómico: no hay columna {@code version}. */
    @Modifying
    @Query("update Contingencia c set c.facturasAfectadas = c.facturasAfectadas + 1"
            + " where c.id = :id")
    void sumarFacturaAfectada(@Param("id") UUID id);
}
