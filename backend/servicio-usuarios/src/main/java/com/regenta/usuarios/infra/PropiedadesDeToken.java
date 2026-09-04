package com.regenta.usuarios.infra;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion de {@code regenta.jwt}: lo que se firma y por cuanto vale. */
@ConfigurationProperties(prefix = "regenta.jwt")
public class PropiedadesDeToken {

    /** El mismo secreto con el que el gateway valida. Minimo 32 caracteres. */
    private String secreto = "";

    private String emisor = "regenta";

    /** Corto a proposito: es lo que acota el daño de un token robado. */
    private Duration vigenciaAcceso = Duration.ofMinutes(15);

    /** Largo, pero rota en cada uso y se puede revocar. */
    private Duration vigenciaRefresco = Duration.ofDays(30);

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

    public Duration getVigenciaAcceso() {
        return vigenciaAcceso;
    }

    public void setVigenciaAcceso(Duration vigenciaAcceso) {
        this.vigenciaAcceso = vigenciaAcceso;
    }

    public Duration getVigenciaRefresco() {
        return vigenciaRefresco;
    }

    public void setVigenciaRefresco(Duration vigenciaRefresco) {
        this.vigenciaRefresco = vigenciaRefresco;
    }
}
