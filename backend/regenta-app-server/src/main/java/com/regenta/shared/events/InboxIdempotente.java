package com.regenta.shared.events;

import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La puerta de entrada de todo consumidor: procesa una vez, o no procesa.
 *
 * <p>AMQP entrega <em>at-least-once</em>. Sin esto, un evento repetido factura dos
 * veces, descuenta stock dos veces o cobra dos veces. El registro en el Inbox va en la
 * MISMA transaccion que el efecto: o quedan los dos, o no queda ninguno.
 *
 * <p>El "reclamo" (el insert que decide quien gana la carrera cuando el mismo
 * mensaje llega dos veces a la vez) corre en su PROPIA transaccion fisica,
 * separada de la del efecto. No es un detalle menor: un flush que choca contra
 * la PK dentro de Hibernate deja inservible la sesion completa de esa
 * transaccion, y aunque el codigo atrape la excepcion en Java, Spring igual
 * lanza {@code UnexpectedRollbackException} al intentar el commit normal del
 * metodo -asi se haya marcado rollback-only a mano o no, eso ya paso solo-.
 * Confirmado en logs de produccion: cada choque de llave tumbaba TODO el
 * listener de la saga de ventas (HU-038), dejando la venta trabada en
 * PENDIENTE_STOCK para siempre. Aislando el reclamo en su propia transaccion,
 * un choque ahi se queda ahi: la transaccion del efecto (si este reclamo si
 * gano la carrera) nunca se entera.
 */
public class InboxIdempotente {

    private static final Logger log = LoggerFactory.getLogger(InboxIdempotente.class);

    private final InboxRepositorio inbox;
    private final TransactionTemplate reclamoEnTransaccionPropia;

    public InboxIdempotente(InboxRepositorio inbox, PlatformTransactionManager transacciones) {
        this.inbox = inbox;
        DefaultTransactionDefinition definicion = new DefaultTransactionDefinition();
        definicion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definicion.setName("inbox-reclamo");
        this.reclamoEnTransaccionPropia = new TransactionTemplate(transacciones, definicion);
    }

    /**
     * Ejecuta el efecto solo si este mensaje no se habia procesado.
     *
     * @return true si se proceso ahora, false si era repetido y se descarto
     */
    @Transactional
    public boolean procesarUnaVez(UUID mensajeId, UUID negocioId, String tipoEvento,
                                  String payload, Consumer<String> efecto) {
        if (!reclamar(mensajeId, negocioId, tipoEvento, payload)) {
            return false;
        }
        efecto.accept(payload);
        InboxEvento registro = inbox.findById(mensajeId).orElseThrow();
        registro.marcarProcesado();
        inbox.save(registro);
        return true;
    }

    /**
     * El insert que decide la carrera, en su propia transaccion fisica
     * (REQUIRES_NEW): si choca contra la PK, el rollback se queda contenido
     * aqui y no se lleva puesta la transaccion del llamador.
     */
    private boolean reclamar(UUID mensajeId, UUID negocioId, String tipoEvento, String payload) {
        Boolean reclamado = reclamoEnTransaccionPropia.execute(estado -> {
            if (inbox.existsById(mensajeId)) {
                log.debug("Evento {} ({}) repetido: se descarta", mensajeId, tipoEvento);
                return false;
            }
            try {
                inbox.saveAndFlush(new InboxEvento(mensajeId, negocioId, tipoEvento, payload));
                return true;
            } catch (DataIntegrityViolationException e) {
                log.debug("Evento {} ({}) llego dos veces a la vez: se descarta", mensajeId, tipoEvento);
                estado.setRollbackOnly();
                return false;
            }
        });
        return Boolean.TRUE.equals(reclamado);
    }
}
