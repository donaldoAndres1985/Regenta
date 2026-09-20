package com.regenta.comun.bbdd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * HU-126 criterio 4. Bajo {@code FORCE ROW LEVEL SECURITY}, {@code textlike}/
 * {@code texticlike} (LIKE/ILIKE) no son {@code LEAKPROOF} por defecto, así
 * que el planificador nunca baja una búsqueda por nombre parcial a un índice
 * GIN trigram: sigue funcionando, por el índice de {@code negocio_id}, pero
 * deja de ser sub-lineal. Marcarlas es un paso de instalación del cluster
 * (ningún {@code reg_*} es superusuario, ver {@code docker/postgres/init-databases.sql}),
 * así que un entorno donde se olvidó no puede fallar el arranque —solo
 * queda más lento— pero tiene que quedar dicho en el log.
 */
public class VerificadorDeLeakproof implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(VerificadorDeLeakproof.class);

    private final JdbcTemplate jdbc;

    public VerificadorDeLeakproof(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!leakproof()) {
            LOG.warn("La busqueda por nombre parcial funciona, pero no usa el indice trigram: "
                    + "faltan ALTER FUNCTION pg_catalog.textlike/texticlike(text,text) LEAKPROOF "
                    + "en esta base (ver docker/postgres/init-databases.sql).");
        }
    }

    boolean leakproof() {
        Boolean resultado = jdbc.queryForObject(
                "SELECT bool_and(proleakproof) FROM pg_proc WHERE proname IN ('textlike', 'texticlike')",
                Boolean.class);
        return Boolean.TRUE.equals(resultado);
    }
}
