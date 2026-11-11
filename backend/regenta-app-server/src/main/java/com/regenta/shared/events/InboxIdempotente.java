package com.regenta.shared.events;

import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * La puerta de entrada de todo consumidor: procesa una vez, o no procesa.
 *
 * <p>AMQP entrega <em>at-least-once</em>. Sin esto, un evento repetido factura dos
 * veces, descuenta stock dos veces o cobra dos veces. El registro en el Inbox va en la
 * MISMA transaccion que el efecto: o quedan los dos, o no queda ninguno.
 */
public class InboxIdempotente {

    private static final Logger log = LoggerFactory.getLogger(InboxIdempotente.class);

    private final InboxRepositorio inbox;

    public InboxIdempotente(InboxRepositorio inbox) {
        this.inbox = inbox;
    }

    /**
     * Ejecuta el efecto solo si este mensaje no se habia procesado.
     *
     * @return true si se proceso ahora, false si era repetido y se descarto
     */
    @Transactional
    public boolean procesarUnaVez(UUID mensajeId, UUID negocioId, String tipoEvento,
                                  String payload, Consumer<String> efecto) {
        if (inbox.existsById(mensajeId)) {
            log.debug("Evento {} ({}) repetido: se descarta", mensajeId, tipoEvento);
            return false;
        }
        InboxEvento registro = new InboxEvento(mensajeId, negocioId, tipoEvento, payload);
        try {
            // saveAndFlush para que el choque contra la PK ocurra aqui y no al final:
            // si dos consumidores reciben el mismo mensaje a la vez, uno de los dos
            // tiene que perder.
            inbox.saveAndFlush(registro);
        } catch (DataIntegrityViolationException e) {
            // El flush ya dejo la sesion de Hibernate de esta transaccion inservible:
            // seguir devolviendo false sin mas no alcanza, porque Spring de todas
            // formas intenta hacer commit al volver -y ESE commit es el que revienta
            // con la sesion rota, no el insert. Hay que decirle a Spring que haga
            // rollback en vez de intentar el commit: no se pierde nada (el efecto
            // real todavia no corrio), y el otro consumidor que si gano la carrera
            // ya dejo su propio registro en el inbox en su propia transaccion.
            log.debug("Evento {} ({}) llego dos veces a la vez: se descarta", mensajeId, tipoEvento);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
        efecto.accept(payload);
        registro.marcarProcesado();
        inbox.save(registro);
        return true;
    }
}
