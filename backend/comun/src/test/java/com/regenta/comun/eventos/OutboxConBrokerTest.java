package com.regenta.comun.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * HU-008 criterio 3 . El camino feliz, contra RabbitMQ de verdad.
 *
 * <p>Lo que importa comprobar aqui, ademas de que el evento salga, es que salga con el
 * {@code message-id} igual al id del Outbox: ese id es el que el consumidor usa como
 * clave del Inbox, y sin el la idempotencia no tiene de donde agarrarse.
 */
class OutboxConBrokerTest extends BaseConPostgres {

    private static final UUID NEGOCIO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String COLA = "prueba.venta_completada";

    /** Mismo motivo que el de PostgreSQL: uno solo, arrancado a mano. */
    static final RabbitMQContainer RABBIT =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry r) {
        r.add("spring.rabbitmq.host", RABBIT::getHost);
        r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        r.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        r.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        r.add("spring.application.name", () -> "servicio-ventas");
    }

    @Autowired
    private RegistroDeEventos registro;

    @Autowired
    private OutboxRepositorio outbox;

    @Autowired
    private PublicadorDeOutbox publicador;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin admin;

    @Autowired
    private TransactionTemplate enTransaccion;

    @Test
    @DisplayName("criterio 3 . el publicador saca los pendientes y los marca PUBLICADO")
    void publicaYMarca() {
        Queue cola = QueueBuilder.durable(COLA).build();
        TopicExchange exchange = new TopicExchange("regenta.eventos", true, false);
        Binding binding = BindingBuilder.bind(cola).to(exchange).with("venta_completada");
        admin.declareExchange(exchange);
        admin.declareQueue(cola);
        admin.declareBinding(binding);

        outbox.deleteAll();
        UUID venta = UUID.randomUUID();
        enTransaccion.executeWithoutResult(estado ->
                registro.registrar(NEGOCIO, "Venta", venta, "venta_completada",
                        Map.of("total", 1984920)));

        int publicados = enTransaccion.execute(estado -> publicador.publicarPendientes());
        assertThat(publicados).isEqualTo(1);

        OutboxEvento evento = outbox.findAll().get(0);
        assertThat(evento.getEstado()).isEqualTo(EstadoOutbox.PUBLICADO);
        assertThat(evento.getPublicadoEn()).isNotNull();

        Message recibido = rabbitTemplate.receive(COLA, 5000);
        assertThat(recibido).as("el evento no llego a la cola").isNotNull();
        assertThat(recibido.getMessageProperties().getMessageId())
                .as("el message-id debe ser el id del outbox: es la clave del inbox")
                .isEqualTo(evento.getId().toString());
        assertThat(recibido.getMessageProperties().getHeaders())
                .containsEntry("negocio_id", NEGOCIO.toString())
                .containsEntry("tipo_evento", "venta_completada")
                // HU-101: servicio-auditoria escucha todos los eventos y necesita saber de
                // qué servicio vino cada uno, algo que ningún payload de negocio declara.
                .containsEntry("servicio_origen", "servicio-ventas");
        assertThat(new String(recibido.getBody())).contains("1984920");
    }
}
