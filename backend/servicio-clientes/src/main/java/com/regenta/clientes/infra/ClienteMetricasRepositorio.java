package com.regenta.clientes.infra;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.regenta.clientes.domain.ClienteMetricas;

public interface ClienteMetricasRepositorio extends JpaRepository<ClienteMetricas, UUID> {

    /**
     * Suma una compra a la proyeccion del cliente, atomica: dos eventos a la vez
     * para el mismo cliente no se pisan (no hay columna {@code version} que
     * proteja un read-modify-write). La RLS ya acota al negocio; el
     * {@code WITH CHECK} de la politica exige que {@code negocioId} sea el del
     * contexto.
     */
    @Modifying
    @Query(value = """
            insert into crm.cliente_metricas
                (cliente_id, negocio_id, total_documentos, monto_total, ticket_promedio,
                 primera_compra_en, ultima_compra_en, actualizado_en)
            values (:clienteId, :negocioId, 1, :monto, :monto, :ocurrido, :ocurrido, now())
            on conflict (cliente_id) do update set
                total_documentos  = crm.cliente_metricas.total_documentos + 1,
                monto_total       = crm.cliente_metricas.monto_total + excluded.monto_total,
                ticket_promedio   = (crm.cliente_metricas.monto_total + excluded.monto_total)
                                    / (crm.cliente_metricas.total_documentos + 1),
                primera_compra_en = least(crm.cliente_metricas.primera_compra_en,
                                          excluded.primera_compra_en),
                ultima_compra_en  = greatest(crm.cliente_metricas.ultima_compra_en,
                                             excluded.ultima_compra_en),
                actualizado_en    = now()
            """, nativeQuery = true)
    void registrarCompra(@Param("clienteId") UUID clienteId, @Param("negocioId") UUID negocioId,
            @Param("monto") BigDecimal monto, @Param("ocurrido") OffsetDateTime ocurrido);

    /**
     * Ajusta la proyeccion a la baja cuando una venta se anula. Nunca por debajo
     * de cero: si el {@code venta_completada} original no llego a procesarse, un
     * {@code venta_anulada} no deja la metrica en negativo.
     */
    @Modifying
    @Query(value = """
            update crm.cliente_metricas set
                total_documentos = greatest(total_documentos - 1, 0),
                monto_total      = greatest(monto_total - :monto, 0),
                ticket_promedio  = case when greatest(total_documentos - 1, 0) = 0 then 0
                                        else greatest(monto_total - :monto, 0)
                                             / greatest(total_documentos - 1, 0) end,
                actualizado_en   = now()
            where cliente_id = :clienteId
            """, nativeQuery = true)
    void revertirCompra(@Param("clienteId") UUID clienteId, @Param("monto") BigDecimal monto);
}
