package com.regenta.clientes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clientes, cartera y metricas del CRM.
 *
 * <p>Esquema propio: {@code crm}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class ClientesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientesApplication.class, args);
    }
}
