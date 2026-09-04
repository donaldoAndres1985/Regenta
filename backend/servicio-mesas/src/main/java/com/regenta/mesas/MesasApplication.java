package com.regenta.mesas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Patron Comanda: salones, mesas y su estado.
 *
 * <p>Esquema propio: {@code mesas}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class MesasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MesasApplication.class, args);
    }
}
