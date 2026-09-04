package com.regenta.comun.eventos;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Con solo agregar la dependencia, el servicio tiene Outbox e Inbox andando.
 *
 * <p>Las entidades y los repositorios de la libreria viven en otro paquete que el del
 * servicio, asi que hay que decirle a Spring donde estan: por eso el EntityScan y el
 * EnableJpaRepositories apuntan aqui.
 */
@AutoConfiguration
@EnableConfigurationProperties(PropiedadesEventos.class)
@EntityScan(basePackageClasses = OutboxEvento.class)
@EnableJpaRepositories(basePackageClasses = OutboxRepositorio.class)
@EnableScheduling
public class EventosAutoConfiguracion {

    @Bean
    public RegistroDeEventos registroDeEventos(OutboxRepositorio outbox,
                                               com.fasterxml.jackson.databind.ObjectMapper json) {
        return new RegistroDeEventos(outbox, json);
    }

    @Bean
    public InboxIdempotente inboxIdempotente(InboxRepositorio inbox) {
        return new InboxIdempotente(inbox);
    }

    @Bean
    public TopicExchange exchangeDeEventos(PropiedadesEventos p) {
        return new TopicExchange(p.getExchange(), true, false);
    }

    @Bean
    public TopicExchange exchangeDeMuertos(PropiedadesEventos p) {
        return new TopicExchange(p.getExchangeMuertos(), true, false);
    }

    @Bean
    public Queue colaDeMuertos(PropiedadesEventos p) {
        return QueueBuilder.durable(p.getColaMuertos()).build();
    }

    @Bean
    public Binding bindingDeMuertos(Queue colaDeMuertos, TopicExchange exchangeDeMuertos) {
        return BindingBuilder.bind(colaDeMuertos).to(exchangeDeMuertos).with("#");
    }

    @Bean
    public PublicadorDeOutbox publicadorDeOutbox(OutboxRepositorio outbox, RabbitTemplate rabbit,
                                                 PropiedadesEventos propiedades) {
        return new PublicadorDeOutbox(outbox, rabbit, propiedades);
    }

    /**
     * El latido del publicador. Se puede apagar con
     * {@code regenta.eventos.publicador-activo=false} —en tests, o en una instancia
     * que solo consuma— sin perder el bean: publicar a mano sigue funcionando.
     */
    @Bean
    @ConditionalOnProperty(prefix = "regenta.eventos", name = "publicador-activo",
            havingValue = "true", matchIfMissing = true)
    public LatidoDelPublicador latidoDelPublicador(PublicadorDeOutbox publicador) {
        return new LatidoDelPublicador(publicador);
    }

    public static class LatidoDelPublicador {

        private final PublicadorDeOutbox publicador;

        public LatidoDelPublicador(PublicadorDeOutbox publicador) {
            this.publicador = publicador;
        }

        @Scheduled(fixedDelayString = "${regenta.eventos.intervalo:PT2S}")
        public void publicar() {
            publicador.publicarPendientes();
        }
    }
}
