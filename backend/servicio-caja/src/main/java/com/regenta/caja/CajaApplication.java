package com.regenta.caja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sesiones de caja, movimientos y arqueo. Transversal a los tres patrones.
 *
 * <p>Esquema propio: {@code caja}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class CajaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CajaApplication.class, args);
    }
}
