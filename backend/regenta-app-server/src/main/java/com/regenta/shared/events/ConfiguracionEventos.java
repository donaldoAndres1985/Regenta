package com.regenta.shared.events;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionEventos {

    @Bean
    @ConditionalOnMissingBean
    public InboxIdempotente inboxIdempotente(InboxRepositorio inbox) {
        return new InboxIdempotente(inbox);
    }
}
