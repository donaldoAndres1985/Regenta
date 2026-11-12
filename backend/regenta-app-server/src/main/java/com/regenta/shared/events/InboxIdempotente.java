package com.regenta.shared.events;

import java.nio.charset.StandardCharsets;
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
    // DIAGNOSTICO TEMPORAL. Quitar una vez resuelto.
    private static final Logger DIAG = LoggerFactory.getLogger("DIAG_SAGA");

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
     * Ejecuta el efecto solo si ESTE CONSUMIDOR no habia procesado este mensaje.
     *
     * <p>El mismo evento le llega a todos los consumidores con el mismo id, y la
     * tabla del Inbox es una sola para todo el monolito. Si la llave fuera solo
     * el id del evento, el primer consumidor en llegar lo reclamaria y los demas
     * lo descartarian como repetido sin haber hecho nada: asi se quedaban las
     * ventas en PENDIENTE_STOCK (HU-038). Por eso la llave es por consumidor.
     *
     * @param consumidor nombre estable de quien consume, p. ej. "inventario.ConsumidorDeLaSagaDeVentas"
     * @return true si se proceso ahora, false si era repetido y se descarto
     */
    @Transactional
    public boolean procesarUnaVez(String consumidor, UUID mensajeId, UUID negocioId, String tipoEvento,
                                  String payload, Consumer<String> efecto) {
        UUID eventoId = mensajeId;
        mensajeId = claveDe(consumidor, eventoId);
        DIAG.info("procesarUnaVez() consumidor={} evento={} clave={}", consumidor, eventoId, mensajeId);
        DIAG.info("procesarUnaVez() ENTRA mensajeId={} tipo={}", mensajeId, tipoEvento);
        boolean gane = reclamar(mensajeId, negocioId, tipoEvento, payload);
        DIAG.info("procesarUnaVez() reclamar()={} mensajeId={}", gane, mensajeId);
        if (!gane) {
            DIAG.info("procesarUnaVez() SALE false (no gano el reclamo) mensajeId={}", mensajeId);
            return false;
        }
        DIAG.info("procesarUnaVez() llamando efecto.accept mensajeId={}", mensajeId);
        efecto.accept(payload);
        DIAG.info("procesarUnaVez() efecto.accept termino OK mensajeId={}", mensajeId);
        InboxEvento registro = inbox.findById(mensajeId).orElseThrow();
        registro.marcarProcesado();
        inbox.save(registro);
        DIAG.info("procesarUnaVez() SALE true (marcado procesado) mensajeId={}", mensajeId);
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
                DIAG.info("reclamar() existsById=true, ya existia mensajeId={}", mensajeId);
                log.debug("Evento {} ({}) repetido: se descarta", mensajeId, tipoEvento);
                return false;
            }
            try {
                inbox.saveAndFlush(new InboxEvento(mensajeId, negocioId, tipoEvento, payload));
                DIAG.info("reclamar() saveAndFlush OK mensajeId={}", mensajeId);
                return true;
            } catch (DataIntegrityViolationException e) {
                DIAG.info("reclamar() saveAndFlush choco: {}: {} mensajeId={}",
                        e.getClass().getSimpleName(), e.getMessage(), mensajeId);
                log.debug("Evento {} ({}) llego dos veces a la vez: se descarta", mensajeId, tipoEvento);
                estado.setRollbackOnly();
                return false;
            }
        });
        DIAG.info("reclamar() TransactionTemplate.execute devolvio={} mensajeId={}", reclamado, mensajeId);
        return Boolean.TRUE.equals(reclamado);
    }

    static UUID claveDe(String consumidor, UUID mensajeId) {
        return UUID.nameUUIDFromBytes((consumidor + ":" + mensajeId).getBytes(StandardCharsets.UTF_8));
    }
}
