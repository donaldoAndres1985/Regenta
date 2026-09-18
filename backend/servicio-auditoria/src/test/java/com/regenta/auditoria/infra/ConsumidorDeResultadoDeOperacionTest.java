package com.regenta.auditoria.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.auditoria.BaseDeAuditoria;
import com.regenta.auditoria.aplicacion.ColaDeSincronizacion;
import com.regenta.auditoria.aplicacion.OperacionEntrante;
import com.regenta.auditoria.aplicacion.ResultadoDeOperacion;

/**
 * HU-102 criterio 4: una operación que falla por regla de negocio queda
 * RECHAZADA con el motivo, y no bloquea las demás de la misma cola.
 */
class ConsumidorDeResultadoDeOperacionTest extends BaseDeAuditoria {

    @Autowired
    private ColaDeSincronizacion cola;
    @Autowired
    private ConsumidorDeResultadoDeOperacion consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private OperacionEntrante operacion(String idempotencyKey, long secuencia) {
        return new OperacionEntrante(UUID.randomUUID(), idempotencyKey, secuencia, "Venta",
                UUID.randomUUID(), "CREAR", Map.of("numero", "V-" + secuencia), null,
                OffsetDateTime.now());
    }

    private Message resultado(UUID negocio, UUID operacionId, String resultado, String motivo) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("operacion_sync_resultado");
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("negocio_id", negocio.toString());
            payload.put("operacion_id", operacionId.toString());
            payload.put("resultado", resultado);
            payload.put("motivo", motivo);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 4: queda RECHAZADA con el motivo, sin bloquear las demás de la cola")
    void unRechazoNoBloqueaLasDemas() {
        List<ResultadoDeOperacion> subidas = enContexto(negocio, usuario, Set.of(), () -> cola.subirLote(
                "celular-de-ana", "ANDROID", "1.4.0",
                List.of(operacion("op-1", 1), operacion("op-2", 2), operacion("op-3", 3))));
        UUID op2 = subidas.get(1).id();

        consumidor.recibir(resultado(negocio, op2, "RECHAZADA", "stock insuficiente"));

        assertThat(consultar("select estado from operaciones_sync where id = '" + op2 + "'"))
                .containsExactly("RECHAZADA");
        assertThat(consultar("select error from operaciones_sync where id = '" + op2 + "'"))
                .containsExactly("stock insuficiente");
        // op-1 y op-3 siguen RECIBIDA: el rechazo de op-2 no las tocó.
        assertThat(consultar("select estado from operaciones_sync where negocio_id = '" + negocio
                + "' and idempotency_key in ('op-1', 'op-3') order by idempotency_key"))
                .containsExactly("RECIBIDA", "RECIBIDA");
    }

    @Test
    @DisplayName("Una operación aplicada con éxito queda APLICADA")
    void seMarcaAplicada() {
        UUID id = enContexto(negocio, usuario, Set.of(),
                () -> cola.subirLote("celular-de-ana", "ANDROID", "1.4.0",
                        List.of(operacion("op-ok", 1))).get(0).id());

        consumidor.recibir(resultado(negocio, id, "APLICADA", null));

        assertThat(consultar("select estado from operaciones_sync where id = '" + id + "'"))
                .containsExactly("APLICADA");
    }

    @Test
    @DisplayName("El mismo resultado entregado dos veces no falla")
    void idempotente() {
        UUID id = enContexto(negocio, usuario, Set.of(),
                () -> cola.subirLote("celular-de-ana", "ANDROID", "1.4.0",
                        List.of(operacion("op-idem", 1))).get(0).id());

        consumidor.recibir(resultado(negocio, id, "RECHAZADA", "duplicado"));
        consumidor.recibir(resultado(negocio, id, "RECHAZADA", "duplicado"));

        assertThat(consultar("select estado from operaciones_sync where id = '" + id + "'"))
                .containsExactly("RECHAZADA");
    }
}
