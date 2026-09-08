package com.regenta.compras.infra;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna el siguiente número de un consecutivo por negocio (HU-047).
 *
 * <p>Un solo {@code INSERT ... ON CONFLICT ... RETURNING} lo hace atómico entre
 * solicitudes concurrentes: la fila de {@code consecutivos} queda bloqueada
 * hasta el commit y dos órdenes a la vez no toman el mismo número. La RLS ya
 * acota la fila al negocio de la transacción.
 */
@Component
public class AsignadorDeConsecutivos {

    private final JdbcTemplate jdbc;

    public AsignadorDeConsecutivos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long siguiente(UUID negocioId, String tipo) {
        Long asignado = jdbc.queryForObject(
                "INSERT INTO compras.consecutivos (negocio_id, sucursal_id, tipo, siguiente) "
                        + "VALUES (?, NULL, ?, 2) "
                        + "ON CONFLICT (negocio_id, sucursal_key, tipo) "
                        + "DO UPDATE SET siguiente = compras.consecutivos.siguiente + 1 "
                        + "RETURNING siguiente - 1",
                Long.class, negocioId, tipo);
        return asignado == null ? 1L : asignado;
    }
}
