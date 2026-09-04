package com.regenta.recursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Patron Reserva: recursos, tipos y tarifas.
 *
 * <p>Esquema propio: {@code recursos}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class RecursosApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecursosApplication.class, args);
    }
}
