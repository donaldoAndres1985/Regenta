package com.regenta.auditoria.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.auditoria.BaseDeAuditoria;

/**
 * HU-101 criterio 1: cualquier cambio en una entidad de negocio deja un
 * evento con usuario, servicio, entidad, acción y trace_id. Instrumentar los
 * quince servicios uno por uno no es viable en un solo cambio; en vez de eso,
 * {@link AuditorDeEventos} escucha TODO lo que ya sale por el exchange
 * ({@code key = "#"}) y arma la bitácora a partir de las cabeceras que
 * {@code PublicadorDeOutbox} ya pone en cada mensaje.
 */
class AuditorDeEventosTest extends BaseDeAuditoria {

    @Autowired
    private AuditorDeEventos consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID venta = UUID.randomUUID();

    private Message evento(String messageId, UUID negocio, String tipo, Map<String, String> headers,
            Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey(tipo);
            props.setHeader("negocio_id", negocio.toString());
            props.setHeader("tipo_evento", tipo);
            props.setHeader("agregado_tipo", "Venta");
            props.setHeader("agregado_id", venta.toString());
            props.setHeader("servicio_origen", "servicio-ventas");
            headers.forEach(props::setHeader);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 1: queda un evento con usuario, servicio, entidad, acción y trace_id")
    void registraElEventoCompleto() {
        String mensajeId = UUID.randomUUID().toString();
        consumidor.recibir(evento(mensajeId, negocioA, "venta_completada",
                Map.of("trace_id", "trace-abc-123"),
                Map.of("negocio_id", negocioA.toString(), "usuario_id", usuario.toString(), "venta_id",
                        venta.toString())));

        assertThat(comoElServicio(negocioA, "select servicio, entidad_tipo, accion, trace_id "
                + "from eventos_auditoria where id = '" + mensajeId + "'")).isNotEmpty();
        assertThat(comoElServicio(negocioA,
                "select servicio from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly("servicio-ventas");
        assertThat(comoElServicio(negocioA,
                "select entidad_tipo from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly("Venta");
        assertThat(comoElServicio(negocioA,
                "select entidad_id from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly(venta.toString());
        assertThat(comoElServicio(negocioA,
                "select accion from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly("ACTUALIZAR");
        assertThat(comoElServicio(negocioA,
                "select trace_id from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly("trace-abc-123");
        assertThat(comoElServicio(negocioA,
                "select usuario_id from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly(usuario.toString());
    }

    @Test
    @DisplayName("Un evento de creación se audita con acción CREAR")
    void accionSegunElEvento() {
        String mensajeId = UUID.randomUUID().toString();
        consumidor.recibir(evento(mensajeId, negocioA, "cliente_creado", Map.of(),
                Map.of("negocio_id", negocioA.toString())));

        assertThat(comoElServicio(negocioA,
                "select accion from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly("CREAR");
    }

    @Test
    @DisplayName("El mismo evento entregado dos veces no duplica la bitácora")
    void idempotente() {
        String mensajeId = UUID.randomUUID().toString();
        consumidor.recibir(evento(mensajeId, negocioA, "venta_completada", Map.of(),
                Map.of("negocio_id", negocioA.toString())));
        consumidor.recibir(evento(mensajeId, negocioA, "venta_completada", Map.of(),
                Map.of("negocio_id", negocioA.toString())));

        assertThat(contar("select count(*) from eventos_auditoria where id = '" + mensajeId + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve el evento del primero")
    void aislamiento() {
        String mensajeId = UUID.randomUUID().toString();
        consumidor.recibir(evento(mensajeId, negocioA, "venta_completada", Map.of(),
                Map.of("negocio_id", negocioA.toString())));

        assertThat(comoElServicio(negocioB,
                "select count(*) from eventos_auditoria where id = '" + mensajeId + "'"))
                .containsExactly("0");
    }
}
