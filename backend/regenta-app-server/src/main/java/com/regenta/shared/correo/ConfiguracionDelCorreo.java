package com.regenta.shared.correo;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * HU-122 · Con {@code spring.mail.host} configurado (por entorno: {@code SMTP_HOST}, usuario y clave),
 * el correo sale por SMTP; sin él, queda en memoria. Las credenciales nunca están en el repo.
 */
@Configuration
public class ConfiguracionDelCorreo {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public CorreoSaliente correoSmtp(JavaMailSender smtp,
            @Value("${regenta.correo.remitente:notificaciones@regenta.app}") String direccion,
            ObjectProvider<RemitenteDelNegocio> remitentes) {
        return new CorreoSmtp(smtp, direccion, remitentes.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(CorreoSaliente.class)
    public CorreoSaliente correoEnMemoria() {
        return new CorreoEnMemoria();
    }
}
