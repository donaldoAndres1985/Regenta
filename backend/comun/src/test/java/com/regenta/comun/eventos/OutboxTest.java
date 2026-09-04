package com.regenta.comun.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * HU-008 . Outbox: atomicidad, resistencia al broker caido y agotamiento de reintentos.
 *
 * <p>Sin broker levantado a proposito. Lo que se prueba aqui es que un bus caido no
 * rompa nada y que el evento sobreviva; el camino feliz con RabbitMQ real esta en
 * {@link OutboxConBrokerTest}.
 */
class OutboxTest extends BaseConPostgres {

    private static final UUID NEGOCIO = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private RegistroDeEventos registro;

    @Autowired
    private OutboxRepositorio outbox;

    @Autowired
    private TransactionTemplate enTransaccion;

    @BeforeEach
    void limpiar() {
        outbox.deleteAll();
    }

    /** Un RabbitTemplate que falla siempre, como si el broker estuviera caido. */
    private static class BrokerCaido extends RabbitTemplate {

        final List<String> intentos = new ArrayList<>();

        BrokerCaido() {
            super(new CachingConnectionFactory("localhost", 1));
        }

        @Override
        public void send(String exchange, String routingKey, Message message) {
            intentos.add(exchange);
            throw new AmqpConnectException(new java.net.ConnectException("conexion rechazada"));
        }
    }

    private PublicadorDeOutbox publicadorCon(RabbitTemplate plantilla, int maximoIntentos) {
        PropiedadesEventos p = new PropiedadesEventos();
        p.setMaximoIntentos(maximoIntentos);
        return new PublicadorDeOutbox(outbox, plantilla, p);
    }

    @Test
    @DisplayName("criterio 1 . el evento queda en el outbox dentro de la misma transaccion")
    void elEventoSeGuardaConElAgregado() {
        UUID agregado = UUID.randomUUID();

        enTransaccion.executeWithoutResult(estado ->
                registro.registrar(NEGOCIO, "Venta", agregado, "venta_completada",
                        Map.of("total", 1984920)));

        List<OutboxEvento> todos = outbox.findAll();
        assertThat(todos).hasSize(1);
        assertThat(todos.get(0).getTipoEvento()).isEqualTo("venta_completada");
        assertThat(todos.get(0).getEstado()).isEqualTo(EstadoOutbox.PENDIENTE);
        assertThat(todos.get(0).getPayload()).contains("1984920");
    }

    @Test
    @DisplayName("atomicidad . si falla el guardado del agregado, tampoco queda el evento")
    void sinCommitNoQuedaEvento() {
        assertThatThrownBy(() ->
                enTransaccion.executeWithoutResult(estado -> {
                    registro.registrar(NEGOCIO, "Venta", UUID.randomUUID(), "venta_completada",
                            Map.of("total", 1));
                    // Lo que en produccion seria un constraint que revienta al guardar la venta.
                    throw new IllegalStateException("fallo al guardar el agregado");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(outbox.findAll())
                .as("el evento sobrevivio a una transaccion abortada")
                .isEmpty();
    }

    @Test
    @DisplayName("registrar fuera de una transaccion no esta permitido")
    void exigeTransaccion() {
        // Registrar el evento en su propia transaccion seria justo el problema que el
        // Outbox viene a resolver, asi que la libreria lo prohibe en vez de permitirlo.
        assertThatThrownBy(() -> registro.registrar(NEGOCIO, "Venta", UUID.randomUUID(),
                "venta_completada", Map.of()))
                .isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);
    }

    @Test
    @DisplayName("criterio 2 . con el broker caido la operacion termina bien y el evento queda pendiente")
    void conElBrokerCaidoElEventoEspera() {
        enTransaccion.executeWithoutResult(estado ->
                registro.registrar(NEGOCIO, "Venta", UUID.randomUUID(), "venta_completada", Map.of()));

        BrokerCaido caido = new BrokerCaido();
        int publicados = enTransaccion.execute(estado ->
                publicadorCon(caido, 10).publicarPendientes());

        assertThat(publicados).isZero();
        OutboxEvento evento = outbox.findAll().get(0);
        assertThat(evento.getEstado()).isEqualTo(EstadoOutbox.PENDIENTE);
        assertThat(evento.getIntentos()).isEqualTo(1);
        assertThat(evento.getUltimoError()).isNotBlank();
    }

    @Test
    @DisplayName("criterio 5 . a los 10 intentos queda FALLIDO y se enruta a la cola muerta")
    void alAgotarIntentosVaALaColaMuerta() {
        enTransaccion.executeWithoutResult(estado ->
                registro.registrar(NEGOCIO, "Venta", UUID.randomUUID(), "venta_completada", Map.of()));

        BrokerCaido caido = new BrokerCaido();
        PublicadorDeOutbox publicador = publicadorCon(caido, 10);
        for (int i = 0; i < 10; i++) {
            enTransaccion.execute(estado -> publicador.publicarPendientes());
        }

        OutboxEvento evento = outbox.findAll().get(0);
        assertThat(evento.getIntentos()).isEqualTo(10);
        assertThat(evento.getEstado()).isEqualTo(EstadoOutbox.FALLIDO);
        assertThat(caido.intentos)
                .as("el ultimo intento tambien probo la cola muerta")
                .contains("regenta.eventos.muertos");

        // Y deja de tomarse: un FALLIDO no se reintenta solo.
        int publicados = enTransaccion.execute(estado -> publicador.publicarPendientes());
        assertThat(publicados).isZero();
        assertThat(outbox.findAll().get(0).getIntentos()).isEqualTo(10);
    }
}
