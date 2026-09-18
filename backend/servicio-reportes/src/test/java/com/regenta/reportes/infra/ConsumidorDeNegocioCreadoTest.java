package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.regenta.reportes.BaseDeReportes;
import com.regenta.reportes.aplicacion.ZonaHorariaDeNegocios;

/**
 * HU-097 criterio 3: la zona horaria del negocio llega en negocio_creado
 * (HU-011) y se cachea aquí, porque servicio-reportes no consulta la base de
 * servicio-usuarios.
 */
class ConsumidorDeNegocioCreadoTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeNegocioCreado consumidor;
    @Autowired
    private ZonaHorariaDeNegocios zonas;
    @Autowired
    private ObjectMapper json;

    private final UUID negocio = UUID.randomUUID();

    private Message negocioCreado(String messageId, UUID negocioId, String zona) {
        Map<String, Object> payload = Map.of(
                "negocio_id", negocioId.toString(),
                "nombre_comercial", "La Espiga",
                "plan", "PROFESIONAL",
                "patron", "VENTA_DIRECTA",
                "zona_horaria", zona);
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey("negocio_creado");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Guarda la zona horaria del negocio para que el corte diario la use")
    void guardaLaZonaHoraria() {
        consumidor.recibir(negocioCreado(UUID.randomUUID().toString(), negocio, "America/Mexico_City"));

        String zona = enContexto(negocio, null, Set.of(), () -> zonas.de(negocio));
        assertThat(zona).isEqualTo("America/Mexico_City");
    }

    @Test
    @DisplayName("El mismo evento entregado dos veces no falla ni cambia el resultado")
    void idempotente() {
        String mensajeId = UUID.randomUUID().toString();
        consumidor.recibir(negocioCreado(mensajeId, negocio, "America/Mexico_City"));
        consumidor.recibir(negocioCreado(mensajeId, negocio, "America/Mexico_City"));

        String zona = enContexto(negocio, null, Set.of(), () -> zonas.de(negocio));
        assertThat(zona).isEqualTo("America/Mexico_City");
    }
}
