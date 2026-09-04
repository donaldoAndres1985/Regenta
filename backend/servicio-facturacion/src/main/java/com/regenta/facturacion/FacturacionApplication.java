package com.regenta.facturacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Factura electronica, resoluciones y transmision.
 *
 * <p>Esquema propio: {@code facturacion}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class FacturacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacturacionApplication.class, args);
    }
}
