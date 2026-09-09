package com.regenta.mesas.infra;

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
import com.regenta.mesas.BaseDeMesas;
import com.regenta.mesas.aplicacion.GestionDeMesas;
import com.regenta.mesas.aplicacion.GestionDeSesionesDeMesa;
import com.regenta.mesas.aplicacion.SolicitudDeApertura;
import com.regenta.mesas.aplicacion.SolicitudDeMesa;

/** HU-082 criterio 3. Al cerrarse la comanda, la mesa queda SUCIA. */
class ConsumidorDeComandasCerradasTest extends BaseDeMesas {

    private static final Set<String> ADMIN = Set.of("MESAS_MESA_VER", "MESAS_MESA_CREAR",
            "MESAS_MESA_EDITAR");

    @Autowired
    private ConsumidorDeComandasCerradas consumidor;
    @Autowired
    private GestionDeMesas mesas;
    @Autowired
    private GestionDeSesionesDeMesa sesiones;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();

    private UUID mesaConSesion() {
        UUID m = enContexto(negocioA, mesero, ADMIN, () -> mesas.crear(new SolicitudDeMesa(null,
                "M-" + UUID.randomUUID().toString().substring(0, 8), null, 4, "CUADRADA", 0, 0, 80,
                80))).id();
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.abrir(m, new SolicitudDeApertura(2)));
        return m;
    }

    private UUID sesionDe(UUID mesaId) {
        return enContexto(negocioA, mesero, ADMIN, () -> sesiones.sesionActual(mesaId)).id();
    }

    private String estadoMesa(UUID mesaId) {
        return consultar("SELECT estado FROM mesas WHERE id = '" + mesaId + "'").get(0);
    }

    private Message comandaCerrada(String messageId, Map<String, Object> extra) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("negocio_id", negocioA.toString());
        payload.putAll(extra);
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(messageId);
            props.setReceivedRoutingKey("comanda_cerrada");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 3: comanda_cerrada con mesa_id cierra la sesión y deja la mesa SUCIA")
    void porMesaId() {
        UUID m = mesaConSesion();
        UUID s = sesionDe(m);

        consumidor.recibir(comandaCerrada(UUID.randomUUID().toString(),
                Map.of("mesa_id", m.toString())));

        assertThat(estadoMesa(m)).isEqualTo("SUCIA");
        assertThat(enContexto(negocioA, mesero, ADMIN, () -> sesiones.ver(s)).estado())
                .isEqualTo("CERRADA");
    }

    @Test
    @DisplayName("comanda_cerrada con sesion_id cierra esa sesión")
    void porSesionId() {
        UUID m = mesaConSesion();
        UUID s = sesionDe(m);

        consumidor.recibir(comandaCerrada(UUID.randomUUID().toString(),
                Map.of("sesion_id", s.toString())));

        assertThat(estadoMesa(m)).isEqualTo("SUCIA");
    }

    @Test
    @DisplayName("La misma comanda cerrada dos veces no reabre nada (Inbox)")
    void idempotente() {
        UUID m = mesaConSesion();
        UUID s = sesionDe(m);
        String id = UUID.randomUUID().toString();

        consumidor.recibir(comandaCerrada(id, Map.of("sesion_id", s.toString())));
        consumidor.recibir(comandaCerrada(id, Map.of("sesion_id", s.toString())));

        assertThat(contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + s
                + "' AND tipo_evento = 'sesion_mesa_cerrada'")).isEqualTo(1);
    }

    @Test
    @DisplayName("comanda_cerrada de una mesa sin sesión viva no falla")
    void mesaSinSesion() {
        UUID m = enContexto(negocioA, mesero, ADMIN, () -> mesas.crear(new SolicitudDeMesa(null,
                "M-libre", null, 4, "CUADRADA", 0, 0, 80, 80))).id();

        consumidor.recibir(comandaCerrada(UUID.randomUUID().toString(),
                Map.of("mesa_id", m.toString())));

        assertThat(estadoMesa(m)).isEqualTo("LIBRE");
    }
}
