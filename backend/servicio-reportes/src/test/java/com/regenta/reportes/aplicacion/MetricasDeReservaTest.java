package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
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
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.reportes.BaseDeReportes;
import com.regenta.reportes.infra.ConsumidorDeEstanciasFinalizadas;
import com.regenta.reportes.infra.ConsumidorDeRecursos;

/**
 * HU-099 criterios 1 y 3. Un hotel no se mide con las métricas de una tienda:
 * ocupación, ADR y RevPAR por día, y solo si el negocio es de patrón Reserva.
 *
 * <p>Las tres se calculan sobre estancias ya cerradas. La ocupación de hoy —
 * quién está durmiendo ahora mismo — es una pregunta operativa y la contesta
 * servicio-reservas; esto es el reporte, y un reporte se hace sobre lo que ya
 * pasó.
 */
class MetricasDeReservaTest extends BaseDeReportes {

    private static final Set<String> VER = Set.of("REPORTES_REPORTE_VER");

    @Autowired
    private MetricasDeReserva metricas;
    @Autowired
    private ConsumidorDeRecursos recursos;
    @Autowired
    private ConsumidorDeEstanciasFinalizadas estancias;
    @Autowired
    private ObjectMapper json;

    // Un negocio por test: la base no se limpia entre uno y otro, y un hotel
    // compartido haria que el inventario de habitaciones dependiera del orden
    // en que JUnit decida correrlos.
    private final UUID ferreteria = UUID.randomUUID();
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

    private UUID habitacion(UUID negocio, String codigo) {
        UUID recursoId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("recurso_id", recursoId.toString());
        payload.put("sucursal_id", null);
        payload.put("tipo_recurso_id", tipoDoble.toString());
        payload.put("tipo_recurso_nombre", "Habitación doble");
        payload.put("codigo", codigo);
        payload.put("nombre", "Habitación " + codigo);
        payload.put("capacidad", 2);
        payload.put("estado", "DISPONIBLE");
        payload.put("activo", true);
        payload.put("disponible", true);
        recursos.recibir(mensaje("recurso_creado", payload));
        return recursoId;
    }

    /** Una estancia de {@code noches} noches que empieza en {@code desde}. */
    private void estancia(UUID negocio, UUID recursoId, LocalDate desde, int noches,
            String alojamiento, String consumos) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("reserva_id", UUID.randomUUID().toString());
        payload.put("estancia_id", UUID.randomUUID().toString());
        payload.put("cliente_id", UUID.randomUUID().toString());
        payload.put("recurso_id", recursoId.toString());
        payload.put("tipo_recurso_id", tipoDoble.toString());
        payload.put("desde", desde.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).toString());
        payload.put("hasta", desde.plusDays(noches).atStartOfDay()
                .atOffset(java.time.ZoneOffset.UTC).toString());
        payload.put("noches", noches);
        payload.put("alojamiento", new BigDecimal(alojamiento));
        payload.put("servicios", BigDecimal.ZERO);
        payload.put("consumos", new BigDecimal(consumos));
        payload.put("subtotal", new BigDecimal(alojamiento).add(new BigDecimal(consumos)));
        payload.put("total", new BigDecimal(alojamiento).add(new BigDecimal(consumos)));
        payload.put("check_out_en", desde.plusDays(noches).atTime(11, 0)
                .atOffset(java.time.ZoneOffset.UTC).toString());
        estancias.recibir(mensaje("estancia_finalizada", payload));
    }

    @Test
    @DisplayName("Criterio 1: el hotel ve ocupación, ADR y RevPAR por día")
    void ocupacionAdrYRevparPorDia() {
        UUID hotel = UUID.randomUUID();
        UUID ciento1 = habitacion(hotel, "101");
        habitacion(hotel, "102");
        habitacion(hotel, "103");
        habitacion(hotel, "104");   // 4 habitaciones
        LocalDate lunes = LocalDate.now().minusDays(10);
        // Una estancia de 2 noches a 200.000 la noche, con 50.000 de consumos
        // que no deben ensuciar el ADR.
        estancia(hotel, ciento1, lunes, 2, "400000", "50000");

        List<OcupacionDelDia> dias = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(lunes, lunes.plusDays(1)));

        assertThat(dias).hasSize(2);
        OcupacionDelDia primero = dias.get(0);
        assertThat(primero.fecha()).isEqualTo(lunes);
        assertThat(primero.recursosTotales()).isEqualTo(4);
        assertThat(primero.recursosOcupados()).isEqualTo(1);
        assertThat(primero.ocupacionPct()).isEqualByComparingTo("25");
        assertThat(primero.adr())
                .as("ADR es solo alojamiento: 400.000 / 2 noches, sin los consumos")
                .isEqualByComparingTo("200000");
        assertThat(primero.revpar())
                .as("RevPAR reparte ese ingreso entre TODAS las habitaciones, no solo las vendidas")
                .isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("Criterio 1: dos estancias la misma noche suman ocupación y promedian el ADR")
    void dosEstanciasLaMismaNoche() {
        UUID hotel = UUID.randomUUID();
        UUID ciento1 = habitacion(hotel, "201");
        UUID ciento2 = habitacion(hotel, "202");
        habitacion(hotel, "203");
        habitacion(hotel, "204");
        LocalDate noche = LocalDate.now().minusDays(20);
        estancia(hotel, ciento1, noche, 1, "200000", "0");
        estancia(hotel, ciento2, noche, 1, "300000", "0");

        List<OcupacionDelDia> dias = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(noche, noche));

        assertThat(dias).hasSize(1);
        assertThat(dias.get(0).recursosOcupados()).isEqualTo(2);
        assertThat(dias.get(0).ocupacionPct()).isEqualByComparingTo("50");
        assertThat(dias.get(0).adr()).isEqualByComparingTo("250000");   // 500.000 / 2 vendidas
        assertThat(dias.get(0).revpar()).isEqualByComparingTo("125000"); // 500.000 / 4 totales
    }

    @Test
    @DisplayName("Criterio 3: una ferretería no tiene ocupación que consultar")
    void laFerreteriaNoVeMetricasDeHotel() {
        assertThatThrownBy(() -> enContexto(ferreteria, duenio, "VENTA_DIRECTA", VER,
                () -> metricas.ocupacion(LocalDate.now().minusDays(1), LocalDate.now())))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Un hotel no ve la ocupación del hotel de al lado")
    void aislamientoEntreHoteles() {
        UUID hotel = UUID.randomUUID();
        UUID otroHotel = UUID.randomUUID();
        UUID suya = habitacion(otroHotel, "301");
        LocalDate noche = LocalDate.now().minusDays(30);
        estancia(otroHotel, suya, noche, 1, "150000", "0");

        List<OcupacionDelDia> deEsteHotel = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(noche, noche));

        assertThat(deEsteHotel).isEmpty();
        assertThat(enContexto(otroHotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(noche, noche))).hasSize(1);
    }
}
