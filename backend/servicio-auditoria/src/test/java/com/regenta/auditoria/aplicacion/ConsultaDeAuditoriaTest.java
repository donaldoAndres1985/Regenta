package com.regenta.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

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
import com.regenta.auditoria.infra.AuditorDeEventos;

/** HU-101 criterios 2 y 4. */
class ConsultaDeAuditoriaTest extends BaseDeAuditoria {

    private static final Set<String> VER_AUDITORIA = Set.of("AUDITORIA_BITACORA_VER");

    @Autowired
    private ConsultaDeAuditoria consulta;
    @Autowired
    private AuditorDeEventos consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocio = UUID.randomUUID();

    private Message evento(UUID negocio, String tipo, String traceId, String agregadoTipo) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipo);
            props.setHeader("negocio_id", negocio.toString());
            props.setHeader("agregado_tipo", agregadoTipo);
            props.setHeader("agregado_id", UUID.randomUUID().toString());
            props.setHeader("servicio_origen", "servicio-ventas");
            if (traceId != null) {
                props.setHeader("trace_id", traceId);
            }
            return MessageBuilder.withBody(json.writeValueAsBytes(Map.of("negocio_id", negocio.toString())))
                    .andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 4: un trace_id reconstruye todo lo que cruzó varios servicios")
    void reconstruyeElTrace() {
        String trace = "trace-" + UUID.randomUUID();
        consumidor.recibir(evento(negocio, "venta_completada", trace, "Venta"));
        consumidor.recibir(evento(negocio, "factura_emitida", trace, "Factura"));
        consumidor.recibir(evento(negocio, "venta_completada", "otro-trace", "Venta"));

        List<EventoDeAuditoria> filas =
                enContexto(negocio, null, VER_AUDITORIA, () -> consulta.porTraceId(trace));

        assertThat(filas).hasSize(2);
        assertThat(filas).extracting(EventoDeAuditoria::entidadTipo)
                .containsExactlyInAnyOrder("Venta", "Factura");
    }

    @Test
    @DisplayName("Criterio 2: la base rechaza un UPDATE sobre un evento de auditoría")
    void rechazaLaEdicion() {
        UUID id = UUID.randomUUID();
        ejecutarComoElServicio(negocio, """
                insert into eventos_auditoria (id, negocio_id, servicio, entidad_tipo, entidad_id, accion,
                    resultado, ocurrido_en)
                values ('%s', '%s', 'servicio-ventas', 'Venta', '%s', 'ACTUALIZAR', 'OK', now())
                """.formatted(id, negocio, UUID.randomUUID()));

        Throwable lanzada = catchThrowable(() -> ejecutarComoElServicio(negocio,
                "update eventos_auditoria set servicio = 'otro' where id = '" + id + "'"));

        assertThat(lanzada).isInstanceOf(IllegalStateException.class);
        assertThat(lanzada.getCause()).hasMessageContaining("append-only");
    }

    @Test
    @DisplayName("Criterio 2: la base rechaza un DELETE sobre un evento de auditoría")
    void rechazaElBorrado() {
        UUID id = UUID.randomUUID();
        ejecutarComoElServicio(negocio, """
                insert into eventos_auditoria (id, negocio_id, servicio, entidad_tipo, entidad_id, accion,
                    resultado, ocurrido_en)
                values ('%s', '%s', 'servicio-ventas', 'Venta', '%s', 'ACTUALIZAR', 'OK', now())
                """.formatted(id, negocio, UUID.randomUUID()));

        Throwable lanzada = catchThrowable(
                () -> ejecutarComoElServicio(negocio, "delete from eventos_auditoria where id = '" + id + "'"));

        assertThat(lanzada).isInstanceOf(IllegalStateException.class);
        assertThat(lanzada.getCause()).hasMessageContaining("append-only");
    }
}
