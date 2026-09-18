package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import com.regenta.reportes.infra.ConsumidorDeEstanciasFinalizadas;
import com.regenta.reportes.infra.ConsumidorDeReservasCanceladas;

/**
 * HU-135. {@code hechos_reserva.penalizacion} se quedaba siempre en cero
 * porque nadie escuchaba {@code reserva_cancelada} ni {@code reserva_no_show}.
 */
class CancelacionesYNoShowsTest extends BaseDeReportes {

    private static final Set<String> VER = Set.of("REPORTES_REPORTE_VER");

    @Autowired
    private MetricasDeReserva metricas;
    @Autowired
    private ConsumidorDeReservasCanceladas canceladas;
    @Autowired
    private ConsumidorDeEstanciasFinalizadas estancias;
    @Autowired
    private ObjectMapper json;

    private final UUID duenio = UUID.randomUUID();
    private final UUID tipoDoble = UUID.randomUUID();

    private Message mensaje(String tipoEvento, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipoEvento);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> payloadDeReserva(UUID negocio, int noches, String penalizacion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("reserva_id", UUID.randomUUID().toString());
        payload.put("numero", "R-1");
        payload.put("tipo_recurso_id", tipoDoble.toString());
        payload.put("recurso_id", null);
        payload.put("cliente_id", UUID.randomUUID().toString());
        payload.put("desde", LocalDate.now().plusDays(5).atStartOfDay()
                .atOffset(java.time.ZoneOffset.UTC).toString());
        payload.put("hasta", LocalDate.now().plusDays(5 + noches).atStartOfDay()
                .atOffset(java.time.ZoneOffset.UTC).toString());
        payload.put("noches", noches);
        payload.put("total", new BigDecimal("400000"));
        payload.put("anticipo_requerido", BigDecimal.ZERO);
        payload.put("saldo", BigDecimal.ZERO);
        payload.put("penalizacion", new BigDecimal(penalizacion));
        payload.put("moneda", "COP");
        return payload;
    }

    private void cancelar(UUID negocio, String penalizacion) {
        payload("reserva_cancelada", negocio, penalizacion);
    }

    private void noShow(UUID negocio, String penalizacion) {
        payload("reserva_no_show", negocio, penalizacion);
    }

    private void payload(String tipoEvento, UUID negocio, String penalizacion) {
        Map<String, Object> payload = payloadDeReserva(negocio, 2, penalizacion);
        payload.put("estado", "reserva_no_show".equals(tipoEvento) ? "NO_SHOW" : "CANCELADA");
        canceladas.recibir(mensaje(tipoEvento, payload));
    }

    @Test
    @DisplayName("Criterio 1: una cancelación con penalización queda con estado_final CANCELADA")
    void cancelacionConPenalizacion() {
        UUID hotel = UUID.randomUUID();
        cancelar(hotel, "50000");

        CancelacionesDelPeriodo r = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.cancelaciones(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        assertThat(r.canceladas()).isEqualTo(1);
        assertThat(r.noShows()).isEqualTo(0);
        assertThat(r.penalizacionTotal()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("Criterio 2: un no-show se distingue de una cancelación")
    void noShowSeDistingue() {
        UUID hotel = UUID.randomUUID();
        noShow(hotel, "80000");

        CancelacionesDelPeriodo r = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.cancelaciones(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        assertThat(r.noShows()).isEqualTo(1);
        assertThat(r.canceladas()).isEqualTo(0);
        assertThat(r.penalizacionTotal()).isEqualByComparingTo("80000");
    }

    @Test
    @DisplayName("Criterio 3: la tasa de cancelación y la de no-show se ven junto al total del período")
    void tasasDelPeriodo() {
        UUID hotel = UUID.randomUUID();
        cancelar(hotel, "50000");
        noShow(hotel, "80000");
        estanciaFinalizada(hotel);

        CancelacionesDelPeriodo r = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.cancelaciones(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        assertThat(r.totalReservas()).isEqualTo(3);
        assertThat(r.tasaCancelacion()).isEqualByComparingTo(new BigDecimal(1).divide(new BigDecimal(3), 4,
                java.math.RoundingMode.HALF_UP));
        assertThat(r.tasaNoShow()).isEqualByComparingTo(new BigDecimal(1).divide(new BigDecimal(3), 4,
                java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Criterio 4: una reserva cancelada no cuenta como ocupada")
    void canceladaNoCuentaComoOcupada() {
        UUID hotel = UUID.randomUUID();
        LocalDate desde = LocalDate.now().plusDays(5);
        cancelar(hotel, "50000");

        assertThat(enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(desde, desde.plusDays(3)))).isEmpty();
    }

    @Test
    @DisplayName("Un hotel no ve las cancelaciones del hotel de al lado")
    void aislamientoEntreHoteles() {
        UUID hotel = UUID.randomUUID();
        UUID otroHotel = UUID.randomUUID();
        cancelar(otroHotel, "60000");

        CancelacionesDelPeriodo deEsteHotel = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.cancelaciones(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));
        CancelacionesDelPeriodo delOtro = enContexto(otroHotel, duenio, "RESERVA", VER,
                () -> metricas.cancelaciones(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)));

        assertThat(deEsteHotel.canceladas()).isEqualTo(0);
        assertThat(delOtro.canceladas()).isEqualTo(1);
    }

    private void estanciaFinalizada(UUID negocio) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("reserva_id", UUID.randomUUID().toString());
        payload.put("estancia_id", UUID.randomUUID().toString());
        payload.put("cliente_id", UUID.randomUUID().toString());
        payload.put("recurso_id", UUID.randomUUID().toString());
        payload.put("tipo_recurso_id", tipoDoble.toString());
        LocalDate desde = LocalDate.now();
        payload.put("desde", desde.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString());
        payload.put("hasta", desde.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString());
        payload.put("noches", 1);
        payload.put("alojamiento", new BigDecimal("200000"));
        payload.put("servicios", BigDecimal.ZERO);
        payload.put("consumos", BigDecimal.ZERO);
        payload.put("subtotal", new BigDecimal("200000"));
        payload.put("total", new BigDecimal("200000"));
        payload.put("check_out_en", desde.plusDays(1).atTime(11, 0)
                .atOffset(java.time.ZoneOffset.UTC).toString());
        estancias.recibir(mensaje("estancia_finalizada", payload));
    }
}
