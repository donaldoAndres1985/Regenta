package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
import com.regenta.reportes.BaseDeReportes;
import com.regenta.reportes.infra.ConsumidorDeBloqueosDeRecurso;
import com.regenta.reportes.infra.ConsumidorDeEstanciasFinalizadas;
import com.regenta.reportes.infra.ConsumidorDeRecursos;

/**
 * HU-136. Una habitación en obra no es una habitación que no se vendió: sacarla
 * del denominador es la diferencia entre "ocupamos el 25%" y "ocupamos el 33%
 * de lo que teníamos para vender".
 */
class BloqueosEnLaOcupacionTest extends BaseDeReportes {

    private static final Set<String> VER = Set.of("REPORTES_REPORTE_VER");

    @Autowired
    private MetricasDeReserva metricas;
    @Autowired
    private ConsumidorDeRecursos recursos;
    @Autowired
    private ConsumidorDeBloqueosDeRecurso bloqueos;
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

    /** Un bloqueo [desde, hasta) sobre un recurso. Devuelve su id. */
    private UUID bloquear(UUID negocio, UUID recursoId, LocalDate desde, LocalDate hasta,
            String motivo) {
        UUID bloqueoId = UUID.randomUUID();
        bloqueos.recibir(mensaje("bloqueo_recurso_creado",
                payloadDeBloqueo(negocio, bloqueoId, recursoId, desde, hasta, motivo)));
        return bloqueoId;
    }

    private void levantar(UUID negocio, UUID bloqueoId, UUID recursoId, LocalDate desde,
            LocalDate hasta, String motivo) {
        bloqueos.recibir(mensaje("bloqueo_recurso_levantado",
                payloadDeBloqueo(negocio, bloqueoId, recursoId, desde, hasta, motivo)));
    }

    private Map<String, Object> payloadDeBloqueo(UUID negocio, UUID bloqueoId, UUID recursoId,
            LocalDate desde, LocalDate hasta, String motivo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("bloqueo_id", bloqueoId.toString());
        payload.put("recurso_id", recursoId.toString());
        payload.put("tipo_recurso_id", tipoDoble.toString());
        payload.put("sucursal_id", null);
        payload.put("desde", desde.atStartOfDay().atOffset(ZoneOffset.UTC).toString());
        payload.put("hasta", hasta.atStartOfDay().atOffset(ZoneOffset.UTC).toString());
        payload.put("motivo", motivo);
        payload.put("detalle", "Pintura");
        return payload;
    }

    private void estancia(UUID negocio, UUID recursoId, LocalDate desde, int noches,
            String alojamiento) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("reserva_id", UUID.randomUUID().toString());
        payload.put("estancia_id", UUID.randomUUID().toString());
        payload.put("cliente_id", UUID.randomUUID().toString());
        payload.put("recurso_id", recursoId.toString());
        payload.put("tipo_recurso_id", tipoDoble.toString());
        payload.put("desde", desde.atStartOfDay().atOffset(ZoneOffset.UTC).toString());
        payload.put("hasta", desde.plusDays(noches).atStartOfDay().atOffset(ZoneOffset.UTC).toString());
        payload.put("noches", noches);
        payload.put("alojamiento", new BigDecimal(alojamiento));
        payload.put("servicios", BigDecimal.ZERO);
        payload.put("consumos", BigDecimal.ZERO);
        payload.put("subtotal", new BigDecimal(alojamiento));
        payload.put("total", new BigDecimal(alojamiento));
        payload.put("check_out_en", desde.plusDays(noches).atTime(11, 0)
                .atOffset(ZoneOffset.UTC).toString());
        estancias.recibir(mensaje("estancia_finalizada", payload));
    }

    private OcupacionDelDia delDia(UUID hotel, LocalDate dia) {
        List<OcupacionDelDia> dias = enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(dia, dia));
        assertThat(dias).hasSize(1);
        return dias.get(0);
    }

    @Test
    @DisplayName("Criterio 2: la habitación bloqueada no cuenta en el denominador de esa noche")
    void bloqueadaNoCuentaEnElDenominador() {
        UUID hotel = UUID.randomUUID();
        UUID ciento1 = habitacion(hotel, "101");
        UUID enObra = habitacion(hotel, "102");
        habitacion(hotel, "103");
        habitacion(hotel, "104");            // 4 habitaciones, 1 en obra
        LocalDate noche = LocalDate.now().minusDays(40);

        bloquear(hotel, enObra, noche, noche.plusDays(1), "MANTENIMIENTO");
        estancia(hotel, ciento1, noche, 1, "200000");

        OcupacionDelDia dia = delDia(hotel, noche);
        assertThat(dia.recursosTotales())
                .as("las 4 menos la que estuvo en obra")
                .isEqualTo(3);
        assertThat(dia.recursosOcupados()).isEqualTo(1);
        assertThat(dia.ocupacionPct()).isEqualByComparingTo("33.3333");
    }

    @Test
    @DisplayName("Criterio 3: el reporte dice cuántos estuvieron fuera de servicio y por qué")
    void elReporteDiceCuantosYPorQue() {
        UUID hotel = UUID.randomUUID();
        UUID enObra = habitacion(hotel, "201");
        UUID enLimpieza = habitacion(hotel, "202");
        habitacion(hotel, "203");
        LocalDate noche = LocalDate.now().minusDays(50);

        bloquear(hotel, enObra, noche, noche.plusDays(1), "MANTENIMIENTO");
        bloquear(hotel, enLimpieza, noche, noche.plusDays(1), "LIMPIEZA");

        OcupacionDelDia dia = delDia(hotel, noche);
        assertThat(dia.recursosBloqueados()).isEqualTo(2);
        assertThat(dia.motivosBloqueo())
                .containsEntry("MANTENIMIENTO", 1)
                .containsEntry("LIMPIEZA", 1);
        assertThat(dia.recursosTotales()).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 4: levantar el bloqueo devuelve la habitación a las noches siguientes")
    void levantarDevuelveLasNochesSiguientes() {
        UUID hotel = UUID.randomUUID();
        UUID enObra = habitacion(hotel, "301");
        habitacion(hotel, "302");
        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(5);

        UUID bloqueo = bloquear(hotel, enObra, desde, hasta, "MANTENIMIENTO");
        assertThat(delDia(hotel, desde.plusDays(3)).recursosTotales()).isEqualTo(1);

        levantar(hotel, bloqueo, enObra, desde, hasta, "MANTENIMIENTO");

        OcupacionDelDia despues = delDia(hotel, desde.plusDays(3));
        assertThat(despues.recursosTotales()).isEqualTo(2);
        assertThat(despues.recursosBloqueados()).isEqualTo(0);
    }

    @Test
    @DisplayName("Criterio 4: las noches que ya pasaron siguen contando el bloqueo que hubo")
    void lasNochesPasadasNoSeReescriben() {
        UUID hotel = UUID.randomUUID();
        UUID enObra = habitacion(hotel, "401");
        habitacion(hotel, "402");
        LocalDate ayer = LocalDate.now().minusDays(1);

        UUID bloqueo = bloquear(hotel, enObra, ayer, LocalDate.now().plusDays(3), "MANTENIMIENTO");
        levantar(hotel, bloqueo, enObra, ayer, LocalDate.now().plusDays(3), "MANTENIMIENTO");

        assertThat(delDia(hotel, ayer).recursosBloqueados())
                .as("la habitación sí estuvo en obra anoche; eso ya es histórico")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Un hotel no ve los bloqueos del hotel de al lado")
    void aislamientoEntreHoteles() {
        UUID hotel = UUID.randomUUID();
        UUID otroHotel = UUID.randomUUID();
        UUID suya = habitacion(otroHotel, "501");
        habitacion(hotel, "502");
        LocalDate noche = LocalDate.now().minusDays(60);

        bloquear(otroHotel, suya, noche, noche.plusDays(1), "MANTENIMIENTO");

        assertThat(enContexto(hotel, duenio, "RESERVA", VER,
                () -> metricas.ocupacion(noche, noche)))
                .as("el bloqueo del vecino no crea ni toca filas en este hotel")
                .isEmpty();
        assertThat(delDia(otroHotel, noche).recursosBloqueados()).isEqualTo(1);
    }
}
