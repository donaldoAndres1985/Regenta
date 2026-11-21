package com.regenta.compras.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.compras.domain.LineaDeDevolucionAProveedor;

public interface LineaDeDevolucionAProveedorRepositorio extends JpaRepository<LineaDeDevolucionAProveedor, UUID> {

    List<LineaDeDevolucionAProveedor> findByDevolucionId(UUID devolucionId);

    /** HU-178 C4 · Lo ya devuelto de cada línea de una recepción. */
    List<LineaDeDevolucionAProveedor> findByRecepcionLineaIdIn(Collection<UUID> recepcionLineaIds);
}
