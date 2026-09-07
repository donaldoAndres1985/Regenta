package com.regenta.facturacion.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.facturacion.domain.EstadoResolucion;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.domain.TipoDocumento;

import jakarta.persistence.LockModeType;

public interface ResolucionRepositorio extends JpaRepository<Resolucion, UUID> {

    /**
     * La resolución vigente de un tipo y sucursal, bloqueada para emitir:
     * {@code SELECT … FOR UPDATE}. Dos emisiones a la vez sobre la misma
     * resolución se serializan aquí, así los consecutivos salen sin huecos ni
     * repetidos (HU-054 criterios 1 y 2). Se hace en UNA consulta —y no cargando
     * primero sin bloqueo— para que la fila llegue con su versión fresca y el
     * {@code @Version} no dispare un choque optimista espurio.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Resolucion r where r.negocioId = :negocioId"
            + " and r.tipoDocumento = :tipo"
            + " and r.estado = com.regenta.facturacion.domain.EstadoResolucion.VIGENTE"
            + " and ((:sucursalId is null and r.sucursalId is null)"
            + "      or r.sucursalId = :sucursalId)")
    Optional<Resolucion> tomarVigenteParaEmitir(@Param("negocioId") UUID negocioId,
            @Param("tipo") TipoDocumento tipo, @Param("sucursalId") UUID sucursalId);

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
