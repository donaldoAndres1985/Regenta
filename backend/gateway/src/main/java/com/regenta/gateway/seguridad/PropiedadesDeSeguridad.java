package com.regenta.gateway.seguridad;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion de {@code regenta.seguridad} en application.yml. */
@ConfigurationProperties(prefix = "regenta.seguridad")
public class PropiedadesDeSeguridad {

    private final Jwt jwt = new Jwt();

    /** Rutas que se atienden sin token. Todo lo demas exige uno. */
    private List<String> rutasPublicas = List.of("/actuator/health/**", "/actuator/info");

    public Jwt getJwt() {
        return jwt;
    }

    public List<String> getRutasPublicas() {
        return rutasPublicas;
    }

    public void setRutasPublicas(List<String> rutasPublicas) {
        this.rutasPublicas = rutasPublicas;
    }

    public static class Jwt {

        /** Secreto HS256 compartido con el emisor. Minimo 32 caracteres. */
        private String secreto = "";

        /** Emisor esperado en el claim {@code iss}. Vacio desactiva la validacion. */
        private String emisor = "regenta";

        public String getSecreto() {
            return secreto;
        }

        public void setSecreto(String secreto) {
            this.secreto = secreto;
        }

        public String getEmisor() {
            return emisor;
        }

        public void setEmisor(String emisor) {
            this.emisor = emisor;
        }
    }
}
