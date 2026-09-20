package com.regenta.comun.bbdd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.comun.eventos.BaseConPostgres;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * HU-126. Bajo FORCE ROW LEVEL SECURITY, textlike/texticlike no son LEAKPROOF
 * por defecto: el planificador nunca baja una búsqueda por nombre parcial a
 * un índice GIN trigram. Marcarlas es un paso de instalación (ningún reg_*
 * es superusuario); si se olvidó, tiene que quedar dicho en el log, no
 * fallar el arranque.
 */
class VerificadorDeLeakproofTest extends BaseConPostgres {

    @Autowired
    private JdbcTemplate jdbc;

    private VerificadorDeLeakproof verificador() {
        return new VerificadorDeLeakproof(jdbc);
    }

    @AfterEach
    void restaurar() {
        jdbc.execute("ALTER FUNCTION pg_catalog.textlike(text, text) NOT LEAKPROOF");
        jdbc.execute("ALTER FUNCTION pg_catalog.texticlike(text, text) NOT LEAKPROOF");
    }

    @Test
    @DisplayName("Criterio 1 (negativo): sin el paso de instalación, detecta que falta")
    void detectaCuandoFalta() {
        assertThat(verificador().leakproof()).isFalse();
    }

    @Test
    @DisplayName("Criterio 1: con el paso de instalación aplicado, lo detecta")
    void detectaCuandoSeAplico() {
        jdbc.execute("ALTER FUNCTION pg_catalog.textlike(text, text) LEAKPROOF");
        jdbc.execute("ALTER FUNCTION pg_catalog.texticlike(text, text) LEAKPROOF");

        assertThat(verificador().leakproof()).isTrue();
    }

    @Test
    @DisplayName("Criterio 4: si falta el paso de instalación, arrancar deja un aviso en el log")
    void avisaEnElLogSiFalta() {
        Logger registro = (Logger) org.slf4j.LoggerFactory.getLogger(VerificadorDeLeakproof.class);
        ListAppender<ILoggingEvent> apuntes = new ListAppender<>();
        apuntes.start();
        registro.addAppender(apuntes);
        try {
            verificador().run(null);
        } finally {
            registro.detachAppender(apuntes);
        }

        assertThat(apuntes.list).anyMatch(evento -> evento.getLevel() == Level.WARN
                && evento.getFormattedMessage().toLowerCase(java.util.Locale.ROOT).contains("leakproof"));
    }

    @Test
    @DisplayName("Si el paso de instalación ya se aplicó, arrancar no avisa nada")
    void noAvisaSiYaSeAplico() {
        jdbc.execute("ALTER FUNCTION pg_catalog.textlike(text, text) LEAKPROOF");
        jdbc.execute("ALTER FUNCTION pg_catalog.texticlike(text, text) LEAKPROOF");
        Logger registro = (Logger) org.slf4j.LoggerFactory.getLogger(VerificadorDeLeakproof.class);
        ListAppender<ILoggingEvent> apuntes = new ListAppender<>();
        apuntes.start();
        registro.addAppender(apuntes);
        try {
            verificador().run(null);
        } finally {
            registro.detachAppender(apuntes);
        }

        assertThat(apuntes.list).isEmpty();
    }
}
