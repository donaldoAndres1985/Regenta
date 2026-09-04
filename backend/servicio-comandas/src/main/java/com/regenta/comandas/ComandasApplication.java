package com.regenta.comandas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Patron Comanda: pedidos, lineas y cocina.
 *
 * <p>Esquema propio: {@code comandas}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class ComandasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComandasApplication.class, args);
    }
}
