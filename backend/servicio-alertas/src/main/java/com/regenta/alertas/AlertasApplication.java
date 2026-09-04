package com.regenta.alertas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Reglas de alerta y notificaciones.
 *
 * <p>Esquema propio: {@code alertas}, en su propia base. Ninguna consulta de este
 * servicio cruza a la base de otro: lo que necesita de afuera llega por eventos.
 */
@SpringBootApplication
public class AlertasApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertasApplication.class, args);
    }
}
