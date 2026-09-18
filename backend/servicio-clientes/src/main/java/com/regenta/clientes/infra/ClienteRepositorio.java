package com.regenta.clientes.infra;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.TipoDocumento;

public interface ClienteRepositorio extends JpaRepository<Cliente, UUID> {

    boolean existsByNegocioIdAndTipoDocumentoAndNumeroDocumento(
            UUID negocioId, TipoDocumento tipoDocumento, String numeroDocumento);

    /** HU-114 criterio 2: un 409 que no dice cual es el que ya existe obliga a buscarlo a mano. */
    Optional<Cliente> findByNegocioIdAndTipoDocumentoAndNumeroDocumento(
            UUID negocioId, TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByNegocioIdAndTipoDocumentoAndNumeroDocumentoAndIdNot(
            UUID negocioId, TipoDocumento tipoDocumento, String numeroDocumento, UUID id);

    List<Cliente> findByNegocioIdAndEliminadoEnIsNullOrderByNombreDisplayAsc(UUID negocioId);

    /**
     * Busqueda del vendedor (HU-021 criterio 3): por nombre parcial o por
     * numero de documento. Nativa y con {@code ILIKE} a proposito: asi el
     * planificador puede apoyarse en el indice GIN trigram sobre
     * {@code nombre_display} y no hacer scan completo sobre 10.000 clientes.
     * La RLS ya acota al negocio; el {@code termino} llega con los {@code %…%}.
     */
    @Query(value = """
            select * from crm.clientes
            where eliminado_en is null
              and (nombre_display ilike :termino or numero_documento ilike :termino)
            order by nombre_display asc
            """, nativeQuery = true)
    List<Cliente> buscar(@Param("termino") String termino, Pageable limite);

    Optional<Cliente> findByIdAndEliminadoEnIsNull(UUID id);
}
