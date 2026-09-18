package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.reportes.BaseDeReportes;

/**
 * HU-099. El inventario de recursos es el denominador de la ocupación: sin él
 * se sabe cuántas noches se vendieron, pero no sobre cuántas.
 */
class ConsumidorDeRecursosTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeRecursos consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID tipoDoble = UUID.randomUUID();

    private Message evento(String tipoEvento, UUID negocio, UUID recursoId, String codigo,
            String estado, boolean activo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("recurso_id", recursoId.toString());
        payload.put("sucursal_id", null);
        payload.put("tipo_recurso_id", tipoDoble.toString());
        payload.put("tipo_recurso_nombre", "Habitación doble");
        payload.put("codigo", codigo);
        payload.put("nombre", "Habitación " + codigo);
        payload.put("capacidad", 2);
        payload.put("estado", estado);
        payload.put("activo", activo);
        payload.put("disponible", activo && "DISPONIBLE".equals(estado));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipoEvento);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("recurso_creado da de alta el recurso en la dimensión, con su tipo legible")
    void altaDelRecurso() {
        UUID recursoId = UUID.randomUUID();

        consumidor.recibir(evento("recurso_creado", negocioA, recursoId, "101", "DISPONIBLE", true));

        assertThat(comoElServicio(negocioA, "select tipo_recurso_nombre from dim_recurso "
                + "where recurso_id = '" + recursoId + "'")).containsExactly("Habitación doble");
        assertThat(comoElServicio(negocioA, "select activo from dim_recurso where recurso_id = '"
                + recursoId + "'")).containsExactly("t");
    }

    @Test
    @DisplayName("recurso_actualizado no duplica la fila: es catálogo, se hace upsert")
    void actualizarNoDuplica() {
        UUID recursoId = UUID.randomUUID();
        consumidor.recibir(evento("recurso_creado", negocioA, recursoId, "102", "DISPONIBLE", true));

        consumidor.recibir(evento("recurso_actualizado", negocioA, recursoId, "102",
                "MANTENIMIENTO", true));

        assertThat(contar("select count(*) from dim_recurso where recurso_id = '" + recursoId + "'"))
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select estado from dim_recurso "
                + "where recurso_id = '" + recursoId + "'")).containsExactly("MANTENIMIENTO");
        assertThat(comoElServicio(negocioA, "select disponible from dim_recurso where recurso_id = '"
                + recursoId + "'")).containsExactly("f");
    }

    @Test
    @DisplayName("Un recurso en mantenimiento sigue contando para la ocupación; uno eliminado no")
    void mantenimientoCuentaYEliminadoNo() {
        UUID enMantenimiento = UUID.randomUUID();
        UUID eliminado = UUID.randomUUID();
        consumidor.recibir(evento("recurso_creado", negocioA, enMantenimiento, "201", "DISPONIBLE", true));
        consumidor.recibir(evento("recurso_creado", negocioA, eliminado, "202", "DISPONIBLE", true));

        consumidor.recibir(evento("recurso_actualizado", negocioA, enMantenimiento, "201",
                "MANTENIMIENTO", true));
        consumidor.recibir(evento("recurso_eliminado", negocioA, eliminado, "202", "DISPONIBLE", false));

        assertThat(contar("select count(*) from dim_recurso where negocio_id = '" + negocioA
                + "' and tipo_recurso_id = '" + tipoDoble + "' and activo"))
                .as("el de mantenimiento sigue existiendo; el eliminado ya no")
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select activo from dim_recurso where recurso_id = '"
                + eliminado + "'")).containsExactly("f");
    }

    @Test
    @DisplayName("El mismo evento dos veces no cambia nada: pasa por el Inbox")
    void eventoRepetidoNoDuplica() {
        UUID recursoId = UUID.randomUUID();
        Message uno = evento("recurso_creado", negocioA, recursoId, "301", "DISPONIBLE", true);

        consumidor.recibir(uno);
        consumidor.recibir(uno);

        assertThat(contar("select count(*) from dim_recurso where recurso_id = '" + recursoId + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve los recursos del primero")
    void aislamientoPorNegocio() {
        UUID deA = UUID.randomUUID();
        UUID deB = UUID.randomUUID();
        consumidor.recibir(evento("recurso_creado", negocioA, deA, "401", "DISPONIBLE", true));
        consumidor.recibir(evento("recurso_creado", negocioB, deB, "401", "DISPONIBLE", true));

        assertThat(comoElServicio(negocioA, "select count(*) from dim_recurso"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioB, "select recurso_id from dim_recurso"))
                .containsExactly(deB.toString());
    }
}
