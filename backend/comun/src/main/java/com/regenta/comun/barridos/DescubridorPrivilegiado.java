package com.regenta.comun.barridos;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * La conexión con el rol privilegiado del barrido (HU-124 criterio 1): el
 * único rol que ve los pendientes de todos los negocios.
 *
 * <p>No es un bean, a propósito: lo crea y lo cierra {@link EjecutorDeBarridos},
 * y no hay forma de inyectarlo en otra parte de la aplicación. El pool es de
 * solo lectura y de dos conexiones; el rol, en la base, también es solo de
 * lectura ({@code docker/postgres/init-databases.sql}).
 */
class DescubridorPrivilegiado implements DescubridorDeNegocios, AutoCloseable {

    private final HikariDataSource fuente;
    private final JdbcTemplate jdbc;

    DescubridorPrivilegiado(String url, String usuario, String clave) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(usuario);
        config.setPassword(clave);
        config.setReadOnly(true);
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setPoolName("barridos");
        this.fuente = new HikariDataSource(config);
        this.jdbc = new JdbcTemplate(fuente);
    }

    @Override
    public List<UUID> negociosCon(String sqlNegociosPendientes) {
        return jdbc.queryForList(sqlNegociosPendientes, UUID.class);
    }

    @Override
    public void close() {
        fuente.close();
    }
}
