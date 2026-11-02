package com.regenta.shared.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "regenta.seguridad")
public class PropiedadesDeSeguridad {

    private Jwt jwt = new Jwt();
    private List<String> rutasPublicas = new ArrayList<>(List.of(
            "/actuator/health/**",
            "/actuator/info",
            "/api/usuarios/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    ));
    private List<String> corsOrigenes = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:5000",
            "https://*.pages.dev"
    ));

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public List<String> getRutasPublicas() {
        return rutasPublicas;
    }

    public void setRutasPublicas(List<String> rutasPublicas) {
        this.rutasPublicas = rutasPublicas;
    }

    public List<String> getCorsOrigenes() {
        return corsOrigenes;
    }

    public void setCorsOrigenes(List<String> corsOrigenes) {
        this.corsOrigenes = corsOrigenes;
    }

    public static class Jwt {
        private String secreto;
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
