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
 * HU-134. {@code hechos_comanda.mesa_codigo} lleva NULL desde V1 porque nada
 * publicaba el catálogo de mesas: {@code dim_mesa} es lo que le da un código
 * y una zona legibles a un reporte por mesa.
 */
class ConsumidorDeMesasTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeMesas consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();

    private Message evento(String tipoEvento, UUID negocio, UUID mesaId, UUID zonaId,
            String zonaNombre, String codigo, boolean activa) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("mesa_id", mesaId.toString());
        payload.put("zona_id", zonaId == null ? null : zonaId.toString());
        payload.put("zona_nombre", zonaNombre);
        payload.put("codigo", codigo);
        payload.put("nombre", "Mesa " + codigo);
        payload.put("capacidad", 4);
        payload.put("activa", activa);
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
    @DisplayName("mesa_creada da de alta la mesa en la dimensión, con su zona legible")
    void altaDeLaMesa() {
        UUID mesaId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        consumidor.recibir(evento("mesa_creada", negocioA, mesaId, zonaId, "Terraza", "T1", true));

        assertThat(comoElServicio(negocioA, "select zona_nombre from dim_mesa "
                + "where mesa_id = '" + mesaId + "'")).containsExactly("Terraza");
        assertThat(comoElServicio(negocioA, "select codigo from dim_mesa where mesa_id = '"
                + mesaId + "'")).containsExactly("T1");
    }

    @Test
    @DisplayName("mesa_actualizada no duplica la fila: es catálogo, se hace upsert")
    void actualizarNoDuplica() {
        UUID mesaId = UUID.randomUUID();
        consumidor.recibir(evento("mesa_creada", negocioA, mesaId, null, null, "M2", true));

        UUID nuevaZona = UUID.randomUUID();
        consumidor.recibir(evento("mesa_actualizada", negocioA, mesaId, nuevaZona, "Salón", "M2", true));

        assertThat(contar("select count(*) from dim_mesa where mesa_id = '" + mesaId + "'"))
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select zona_nombre from dim_mesa "
                + "where mesa_id = '" + mesaId + "'")).containsExactly("Salón");
    }

    @Test
    @DisplayName("mesa_eliminada marca la mesa como inactiva, no borra la fila")
    void eliminarMarcaInactiva() {
        UUID mesaId = UUID.randomUUID();
        consumidor.recibir(evento("mesa_creada", negocioA, mesaId, null, null, "M3", true));

        consumidor.recibir(evento("mesa_eliminada", negocioA, mesaId, null, null, "M3", false));

        assertThat(contar("select count(*) from dim_mesa where mesa_id = '" + mesaId + "'"))
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select activa from dim_mesa where mesa_id = '"
                + mesaId + "'")).containsExactly("f");
    }

    @Test
    @DisplayName("El mismo evento dos veces no cambia nada: pasa por el Inbox")
    void eventoRepetidoNoDuplica() {
        UUID mesaId = UUID.randomUUID();
        Message uno = evento("mesa_creada", negocioA, mesaId, null, null, "M4", true);

        consumidor.recibir(uno);
        consumidor.recibir(uno);

        assertThat(contar("select count(*) from dim_mesa where mesa_id = '" + mesaId + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve las mesas del primero")
    void aislamientoPorNegocio() {
        UUID deA = UUID.randomUUID();
        UUID deB = UUID.randomUUID();
        consumidor.recibir(evento("mesa_creada", negocioA, deA, null, null, "M5", true));
        consumidor.recibir(evento("mesa_creada", negocioB, deB, null, null, "M5", true));

        assertThat(comoElServicio(negocioA, "select count(*) from dim_mesa")).containsExactly("1");
        assertThat(comoElServicio(negocioB, "select mesa_id from dim_mesa"))
                .containsExactly(deB.toString());
    }
}
