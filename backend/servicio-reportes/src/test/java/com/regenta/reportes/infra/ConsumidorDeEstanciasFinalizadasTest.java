package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
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

/** HU-096 criterio 4 (patrón Reserva). estancia_finalizada alimenta hechos_reserva. */
class ConsumidorDeEstanciasFinalizadasTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeEstanciasFinalizadas consumidor;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID cliente = UUID.randomUUID();
    private final UUID recurso = UUID.randomUUID();
    private final UUID tipoRecurso = UUID.randomUUID();

    private Message estanciaFinalizada(UUID negocio, UUID reservaId) {
        Map<String, Object> payload = Map.of("negocio_id", negocio.toString(), "reserva_id",
                reservaId.toString(), "cliente_id", cliente.toString(), "recurso_id", recurso.toString(),
                "tipo_recurso_id", tipoRecurso.toString(), "noches", 2, "total", "400000", "consumos",
                "50000", "check_out_en", OffsetDateTime.now().toString());
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("estancia_finalizada");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 4: estancia_finalizada inserta el hecho de reserva con el ADR calculado")
    void insertaHechoDeReserva() {
        UUID reservaId = UUID.randomUUID();
        consumidor.recibir(estanciaFinalizada(negocioA, reservaId));

        assertThat(contar("select count(*) from hechos_reserva where reserva_id = '" + reservaId + "'"))
                .isEqualTo(1);
        assertThat(comoElServicio(negocioA,
                "select adr from hechos_reserva where reserva_id = '" + reservaId + "'"))
                .containsExactly("200000.0000"); // 400000 / 2 noches
        assertThat(contar("select count(*) from dim_cliente where cliente_id = '" + cliente + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no ve el hecho del primero")
    void aislamiento() {
        UUID reservaId = UUID.randomUUID();
        consumidor.recibir(estanciaFinalizada(negocioA, reservaId));
        assertThat(comoElServicio(negocioB, "select count(*) from hechos_reserva where reserva_id = '"
                + reservaId + "'")).containsExactly("0");
    }

    @Test
    @DisplayName("HU-097 criterio 1: también actualiza el agregado diario de la estancia")
    void actualizaElAgregadoDiario() {
        UUID reservaId = UUID.randomUUID();
        consumidor.recibir(estanciaFinalizada(negocioA, reservaId));

        assertThat(consultar("select num_documentos::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'RESERVA'"))
                .containsExactly("1");
        assertThat(consultar("select monto_neto::text from agregados_diarios "
                + "where negocio_id = '" + negocioA + "' and patron = 'RESERVA'"))
                .containsExactly("400000.0000");
    }
}
