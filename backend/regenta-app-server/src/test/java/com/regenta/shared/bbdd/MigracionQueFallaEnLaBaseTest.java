package com.regenta.shared.bbdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.PostgresDePrueba;
import com.regenta.shared.bbdd.ConfiguracionMultiSchemaFlyway.FlywayMultiSchemaRunner;
import com.regenta.shared.bbdd.ConfiguracionMultiSchemaFlyway.ModuloMigracion;

/**
 * HU-125 C2 contra PostgreSQL · Si una migración falla, el arranque se detiene (la excepción sale del
 * runner, y con ella la app no levanta ni pasa el healthcheck) y la migración no deja nada a medias.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MigracionQueFallaEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void c2_unaMigracionQueFallaDetieneElArranqueSinDejarNadaAMedias() {
        FlywayMultiSchemaRunner runner = new FlywayMultiSchemaRunner(dataSource,
                List.of(new ModuloMigracion("prueba", "prueba_hu125", "classpath:db/prueba-hu125")));

        assertThatThrownBy(runner::ejecutarMigraciones).isInstanceOf(FlywayException.class);

        // V1 quedó aplicada; de V2 no queda nada: ni el registro, ni la fila, ni la columna.
        assertThat(jdbc.queryForList("SELECT version FROM prueba_hu125.flyway_schema_history "
                + "WHERE success AND version IS NOT NULL ORDER BY installed_rank", String.class)).containsExactly("1");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM prueba_hu125.flyway_schema_history WHERE version = '2'",
                Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM prueba_hu125.pedidos", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM information_schema.columns WHERE table_schema = "
                + "'prueba_hu125' AND table_name = 'pedidos' AND column_name = 'total'", Integer.class)).isZero();

        // Y al volver a arrancar con la misma migración rota, vuelve a detenerse: no se "repara" sola.
        assertThatThrownBy(runner::ejecutarMigraciones).isInstanceOf(FlywayException.class);
    }
}
