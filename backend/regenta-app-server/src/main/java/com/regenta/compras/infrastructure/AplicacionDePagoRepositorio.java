package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.AplicacionDePago;

public interface AplicacionDePagoRepositorio extends JpaRepository<AplicacionDePago, UUID> {

    List<AplicacionDePago> findByCuentaId(UUID cuentaId);

    List<AplicacionDePago> findByPagoId(UUID pagoId);
}
