package com.regenta.compras.infra;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.PagoProveedor;

public interface PagoProveedorRepositorio extends JpaRepository<PagoProveedor, UUID> {

    List<PagoProveedor> findByCuentaIdOrderByFechaAsc(UUID cuentaId);
}
