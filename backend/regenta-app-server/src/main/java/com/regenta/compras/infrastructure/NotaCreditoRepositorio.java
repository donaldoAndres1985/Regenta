package com.regenta.compras.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.compras.domain.NotaCreditoProveedor;

public interface NotaCreditoRepositorio extends JpaRepository<NotaCreditoProveedor, UUID> {

    Optional<NotaCreditoProveedor> findByDevolucionId(UUID devolucionId);

    /** Las notas de un proveedor de la más antigua a la más nueva: así se usa el saldo a favor. */
    List<NotaCreditoProveedor> findByNegocioIdAndProveedorIdOrderByCreadoEnAsc(UUID negocioId, UUID proveedorId);

    /** HU-178 / I5 · El saldo a favor de cada proveedor: {@code [proveedorId, saldo]}, solo los que tienen. */
    @Query("""
            select n.proveedorId, sum(n.saldoAFavor)
            from NotaCreditoProveedor n
            where n.negocioId = :negocioId and n.saldoAFavor > 0
            group by n.proveedorId
            """)
    List<Object[]> saldosAFavorPorProveedor(@Param("negocioId") UUID negocioId);

    @Query("""
            select coalesce(sum(n.saldoAFavor), 0)
            from NotaCreditoProveedor n
            where n.negocioId = :negocioId and n.proveedorId = :proveedorId
            """)
    BigDecimal saldoAFavorDe(@Param("negocioId") UUID negocioId, @Param("proveedorId") UUID proveedorId);
}
