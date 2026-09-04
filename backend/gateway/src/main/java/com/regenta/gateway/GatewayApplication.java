package com.regenta.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Puerta de entrada de Regenta.
 *
 * <p>El gateway enruta y valida el JWT; nada mas. Ninguna regla de negocio vive
 * aqui, y ninguna decision tomada aqui se da por buena rio abajo: cada servicio
 * revalida el plan, el modulo y el negocio por su cuenta.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
