package com.regenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * Aplicación principal del monolito modular de Regenta.
 *
 * <p>Integra los límites de dominio de los microservicios en módulos aislados
 * gestionados por Spring Modulith.
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class RegentaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegentaApplication.class, args);
    }
}
