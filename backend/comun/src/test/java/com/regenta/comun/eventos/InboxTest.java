package com.regenta.comun.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HU-008 criterio 4 . Inbox: el mismo evento dos veces surte efecto una sola vez.
 *
 * <p>RabbitMQ entrega at-least-once. Sin esta tabla, un evento repetido factura dos
 * veces. No es un caso raro: pasa cada vez que un consumidor muere antes del ack.
 */
class InboxTest extends BaseConPostgres {

    private static final UUID NEGOCIO = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private InboxIdempotente inbox;

    @Autowired
    private InboxRepositorio repositorio;

    @Test
    @DisplayName("criterio 4 . el evento repetido se descarta y el efecto no se repite")
    void elEventoRepetidoNoSeProcesaDosVeces() {
        UUID mensaje = UUID.randomUUID();
        AtomicInteger veces = new AtomicInteger();

        boolean primera = inbox.procesarUnaVez(mensaje, NEGOCIO, "venta_completada",
                "{\"total\":100}", payload -> veces.incrementAndGet());
        boolean segunda = inbox.procesarUnaVez(mensaje, NEGOCIO, "venta_completada",
                "{\"total\":100}", payload -> veces.incrementAndGet());

        assertThat(primera).isTrue();
        assertThat(segunda).as("se volvio a procesar un evento repetido").isFalse();
        assertThat(veces.get()).isEqualTo(1);

        InboxEvento registro = repositorio.findById(mensaje).orElseThrow();
        assertThat(registro.getEstado()).isEqualTo(EstadoInbox.PROCESADO);
        assertThat(registro.getProcesadoEn()).isNotNull();
    }

    @Test
    @DisplayName("dos mensajes distintos del mismo evento si se procesan los dos")
    void mensajesDistintosSeProcesan() {
        AtomicInteger veces = new AtomicInteger();

        inbox.procesarUnaVez(UUID.randomUUID(), NEGOCIO, "stock_actualizado", "{}",
                p -> veces.incrementAndGet());
        inbox.procesarUnaVez(UUID.randomUUID(), NEGOCIO, "stock_actualizado", "{}",
                p -> veces.incrementAndGet());

        assertThat(veces.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("si el efecto falla, no queda registrado como procesado")
    void siElEfectoFallaNoQuedaProcesado() {
        UUID mensaje = UUID.randomUUID();

        assertThatThrownBy(() -> inbox.procesarUnaVez(mensaje, NEGOCIO, "venta_completada", "{}",
                payload -> {
                    throw new IllegalStateException("el consumidor reviento");
                }))
                .isInstanceOf(IllegalStateException.class);

        // El registro del inbox y el efecto van en la misma transaccion: si el efecto
        // falla, el mensaje puede volver a entregarse y esta vez procesarse de verdad.
        assertThat(repositorio.findById(mensaje))
                .as("quedo marcado como recibido pese a que el efecto fallo")
                .isEmpty();
    }
}
