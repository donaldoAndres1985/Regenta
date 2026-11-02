package com.regenta.menu.infrastructure;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * CuÃ¡ntos Ã­tems cuelgan de una carta (HU-076 criterio 3). Va por
 * {@link JdbcTemplate} porque la entidad de {@code items_menu} la trae HU-077;
 * aquÃ­ solo hace falta el conteo para no borrar una carta con platos. La RLS
 * acota todo al negocio de la transacciÃ³n.
 */
@Component
public class ConteoDeItemsDeCarta {

    private final JdbcTemplate jdbc;

    public ConteoDeItemsDeCarta(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long itemsDe(UUID cartaId) {
        Long n = jdbc.queryForObject(
                "SELECT count(*) FROM menu.items_menu i "
                        + "JOIN menu.categorias_menu c ON c.id = i.categoria_menu_id "
                        + "WHERE c.carta_id = ? AND i.eliminado_en IS NULL",
                Long.class, cartaId);
        return n == null ? 0L : n;
    }
}
