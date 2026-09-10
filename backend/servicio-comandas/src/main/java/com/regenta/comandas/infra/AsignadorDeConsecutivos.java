package com.regenta.comandas.infra;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * El número de comanda ({@code CMD-0001}, {@code CMD-0002}…). El {@code UPSERT}
 * con {@code ON CONFLICT DO UPDATE} toma el lock de la fila, así que dos meseros
 * a la vez no se llevan el mismo número. La RLS acota al negocio de la
 * transacción.
 */
@Component
public class AsignadorDeConsecutivos {

    private static final String SQL = """
            INSERT INTO comandas.consecutivos (negocio_id, sucursal_id, tipo, prefijo, siguiente)
            VALUES (?, NULL, 'COMANDA', 'CMD-', 2)
            ON CONFLICT (negocio_id, sucursal_key, tipo)
            DO UPDATE SET siguiente = comandas.consecutivos.siguiente + 1
            RETURNING siguiente - 1
            """;

    private final JdbcTemplate jdbc;

    public AsignadorDeConsecutivos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String siguienteNumeroDeComanda(UUID negocioId) {
        Long n = jdbc.queryForObject(SQL, Long.class, negocioId);
        return String.format("CMD-%04d", n == null ? 1 : n);
    }
}
