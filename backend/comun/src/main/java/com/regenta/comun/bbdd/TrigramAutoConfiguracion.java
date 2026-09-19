package com.regenta.comun.bbdd;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Con solo tener la dependencia, cada servicio avisa en su log si a su base
 * le falta el paso de instalación de HU-126 (LEAKPROOF en textlike/texticlike).
 */
@AutoConfiguration
public class TrigramAutoConfiguracion {

    @Bean
    public VerificadorDeLeakproof verificadorDeLeakproof(JdbcTemplate jdbc) {
        return new VerificadorDeLeakproof(jdbc);
    }
}
