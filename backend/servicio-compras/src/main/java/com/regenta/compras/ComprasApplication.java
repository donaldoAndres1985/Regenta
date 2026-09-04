package com.regenta.compras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ordenes de compra, recepciones y cuentas por pagar.
 *
 * <p>Esquema propio: {@code compras}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class ComprasApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComprasApplication.class, args);
    }
}
