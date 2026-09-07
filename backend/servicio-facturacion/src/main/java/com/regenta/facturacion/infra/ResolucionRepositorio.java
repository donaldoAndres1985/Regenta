package com.regenta.facturacion.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.facturacion.domain.EstadoResolucion;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.domain.TipoDocumento;

public interface ResolucionRepositorio extends JpaRepository<Resolucion, UUID> {

    List<Resolucion> findByNegocioIdOrderByVigenteHastaDesc(UUID negocioId);

    Optional<Resolucion> findByIdAndNegocioId(UUID id, UUID negocioId);

    boolean existsByNegocioIdAndNumeroResolucion(UUID negocioId, String numeroResolucion);

    /**
     * La resolución vigente de un tipo y sucursal. {@code sucursalId} nulo =
     * resolución del negocio sin sucursal específica. Solo puede haber una
     * (índice parcial {@code uq_resolucion_vigente}).
     */
    @Query("select r from Resolucion r where r.negocioId = :negocioId"
            + " and r.tipoDocumento = :tipo and r.estado = com.regenta.facturacion.domain"
            + ".EstadoResolucion.VIGENTE"
            + " and ((:sucursalId is null and r.sucursalId is null)"
            + "      or r.sucursalId = :sucursalId)")
    Optional<Resolucion> buscarVigente(@Param("negocioId") UUID negocioId,
            @Param("tipo") TipoDocumento tipo, @Param("sucursalId") UUID sucursalId);

    boolean existsByNegocioIdAndTipoDocumentoAndSucursalIdAndEstado(UUID negocioId,
            TipoDocumento tipoDocumento, UUID sucursalId, EstadoResolucion estado);

    boolean existsByNegocioIdAndTipoDocumentoAndSucursalIdIsNullAndEstado(UUID negocioId,
            TipoDocumento tipoDocumento, EstadoResolucion estado);
}
