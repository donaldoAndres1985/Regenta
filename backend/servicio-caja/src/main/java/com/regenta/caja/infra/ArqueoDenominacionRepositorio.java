package com.regenta.caja.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.caja.domain.ArqueoDenominacion;

public interface ArqueoDenominacionRepositorio extends JpaRepository<ArqueoDenominacion, UUID> {

    List<ArqueoDenominacion> findBySesionIdOrderByDenominacionDesc(UUID sesionId);

    @Query("select coalesce(sum(a.subtotal), 0) from ArqueoDenominacion a where a.sesionId = :sesionId")
    BigDecimal totalContado(@Param("sesionId") UUID sesionId);

    boolean existsBySesionId(UUID sesionId);

    @Modifying
    @Query("delete from ArqueoDenominacion a where a.sesionId = :sesionId")
    void borrarDeLaSesion(@Param("sesionId") UUID sesionId);
}
