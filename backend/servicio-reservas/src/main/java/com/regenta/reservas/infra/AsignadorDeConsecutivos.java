package com.regenta.reservas.infra;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna el siguiente número de un consecutivo por negocio (HU-070).
 *
 * <p>La tabla {@code reservas.consecutivos} tiene otra forma que la de los demás
 * servicios: PK {@code (negocio_id, tipo)}, sin sucursal. Un solo
 * {@code INSERT ... ON CONFLICT ... RETURNING} lo hace atómico entre reservas
 * concurrentes; la RLS acota la fila al negocio de la transacción.
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
                "INSERT INTO reservas.consecutivos (negocio_id, tipo, siguiente) "
                        + "VALUES (?, ?, 2) "
                        + "ON CONFLICT (negocio_id, tipo) "
                        + "DO UPDATE SET siguiente = reservas.consecutivos.siguiente + 1 "
                        + "RETURNING siguiente - 1",
                Long.class, negocioId, tipo);
        return asignado == null ? 1L : asignado;
    }
}
