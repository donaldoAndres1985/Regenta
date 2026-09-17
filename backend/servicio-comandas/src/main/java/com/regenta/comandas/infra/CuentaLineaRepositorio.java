package com.regenta.comandas.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.comandas.domain.CuentaLinea;
import com.regenta.comandas.domain.CuentaLineaId;

public interface CuentaLineaRepositorio extends JpaRepository<CuentaLinea, CuentaLineaId> {

    List<CuentaLinea> findByCuentaId(UUID cuentaId);

    List<CuentaLinea> findByComandaLineaId(UUID comandaLineaId);

    Optional<CuentaLinea> findByCuentaIdAndComandaLineaId(UUID cuentaId, UUID comandaLineaId);

    void deleteByCuentaIdAndComandaLineaId(UUID cuentaId, UUID comandaLineaId);
}
