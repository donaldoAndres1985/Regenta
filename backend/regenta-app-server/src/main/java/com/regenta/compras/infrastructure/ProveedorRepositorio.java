package com.regenta.compras.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, UUID> {

    boolean existsByNegocioIdAndTipoDocumentoAndNumeroDocumento(UUID negocioId,
            TipoDocumentoProveedor tipoDocumento, String numeroDocumento);

    List<Proveedor> findByNegocioIdAndEliminadoEnIsNullOrderByRazonSocialAsc(UUID negocioId);

    Optional<Proveedor> findByIdAndNegocioId(UUID id, UUID negocioId);

    /** HU-166 · Todos los proveedores del negocio, activos e inactivos. */
    List<Proveedor> findByNegocioIdOrderByRazonSocialAsc(UUID negocioId);
}
