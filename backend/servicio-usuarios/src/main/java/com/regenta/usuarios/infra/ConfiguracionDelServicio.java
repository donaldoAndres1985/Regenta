package com.regenta.usuarios.infra;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Las dos piezas sueltas del servicio: como se guardan las claves y que hora es. */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionDelServicio {

    /**
     * BCrypt con coste 12. Sube el coste cuando las maquinas suban: el hash
     * guarda su propio coste, asi que los viejos siguen validando.
     */
    @Bean
    public PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder(12);
    }

    /** Inyectado para que los tests puedan fijar la fecha sin esperar a mañana. */
    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
