package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.regenta.reportes.infra.ConsumidorDeMesas;
import com.regenta.reportes.infra.ConsumidorDePedidosCompletados;

/**
 * HU-099 criterios 2, 3 y 4. Un restaurante se mide por cuántas veces giró
 * cada mesa, cuánto duró cada sentada, cuánto dejó cada comensal y cuánto se
 * demoró la cocina en sacar cada plato.
 */
class MetricasDeComandaTest extends BaseDeReportes {

    private static final Set<String> VER = Set.of("REPORTES_REPORTE_VER");

    @Autowired
    private MetricasDeComanda metricas;
    @Autowired
    private ConsumidorDePedidosCompletados pedidos;
    @Autowired
    private ConsumidorDeMesas mesas;
    @Autowired
    private ObjectMapper json;

    private void mesa(UUID negocio, UUID mesaId, String codigo, String zonaNombre) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("mesa_id", mesaId.toString());
        payload.put("zona_id", UUID.randomUUID().toString());
        payload.put("zona_nombre", zonaNombre);
        payload.put("codigo", codigo);
        payload.put("nombre", "Mesa " + codigo);
        payload.put("capacidad", 4);
        payload.put("activa", true);
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("mesa_creada");
            mesas.recibir(
                    MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final UUID duenio = UUID.randomUUID();
    private final UUID ferreteria = UUID.randomUUID();

    private record Plato(String nombre, String montoNeto, Integer minutosDePreparacion) {
    }

    private void pedido(UUID negocio, UUID mesaId, int comensales, int minutosDeMesa,
            Plato... platos) {
        List<Map<String, Object>> lineas = new java.util.ArrayList<>();
        for (Plato p : platos) {
            Map<String, Object> linea = new LinkedHashMap<>();
            linea.put("item_menu_id", UUID.randomUUID().toString());
            linea.put("nombre", p.nombre());
            linea.put("cantidad", 1);
            linea.put("monto_neto", p.montoNeto());
            linea.put("costo", "0");
            linea.put("tiempo_preparacion_min", p.minutosDePreparacion());
            lineas.add(linea);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("comanda_id", UUID.randomUUID().toString());
        payload.put("mesa_id", mesaId.toString());
        payload.put("sesion_mesa_id", UUID.randomUUID().toString());
        payload.put("usuario_id", UUID.randomUUID().toString());
        payload.put("num_comensales", comensales);
        payload.put("propina", "0");
        payload.put("tiempo_mesa_min", minutosDeMesa);
        payload.put("lineas", lineas);
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("pedido_completado");
            pedidos.recibir(
                    MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 2: rotación de mesas, tiempo medio de mesa y ticket por comensal")
    void rotacionTiempoDeMesaYTicketPorComensal() {
        UUID restaurante = UUID.randomUUID();
        UUID mesa1 = UUID.randomUUID();
        UUID mesa2 = UUID.randomUUID();
        // Mesa 1 se usó dos veces; mesa 2, una. Tres sentadas en dos mesas.
        pedido(restaurante, mesa1, 2, 40, new Plato("Bandeja", "60000", 15));
        pedido(restaurante, mesa1, 3, 80, new Plato("Churrasco", "90000", 20));
        pedido(restaurante, mesa2, 1, 30, new Plato("Sopa", "30000", 10));

        MesasDelPeriodo mesas = enContexto(restaurante, duenio, "COMANDA", VER,
                () -> metricas.mesas(LocalDate.now().minusDays(1), LocalDate.now()));

        assertThat(mesas.comandas()).isEqualTo(3);
        assertThat(mesas.mesasUsadas()).isEqualTo(2);
        assertThat(mesas.rotacion())
                .as("3 sentadas en 2 mesas")
                .isEqualByComparingTo("1.5000");
        assertThat(mesas.tiempoMedioMesaMin())
                .as("(40 + 80 + 30) / 3")
                .isEqualByComparingTo("50.0000");
        assertThat(mesas.comensales()).isEqualTo(6);
        assertThat(mesas.ticketPorComensal())
                .as("180.000 entre 6 comensales")
                .isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("Criterio 4: el tiempo medio de preparación de cada plato sale de las marcas del KDS")
    void tiempoMedioDePreparacionPorPlato() {
        UUID restaurante = UUID.randomUUID();
        UUID mesa = UUID.randomUUID();
        pedido(restaurante, mesa, 2, 50,
                new Plato("Bandeja paisa", "40000", 20),
                new Plato("Limonada", "8000", 4));
        pedido(restaurante, mesa, 2, 50,
                new Plato("Bandeja paisa", "40000", 30),
                new Plato("Limonada", "8000", null));   // nunca pasó por cocina

        List<PreparacionDePlato> platos = enContexto(restaurante, duenio, "COMANDA", VER,
                () -> metricas.preparacion(LocalDate.now().minusDays(1), LocalDate.now()));

        assertThat(platos).extracting(PreparacionDePlato::plato)
                .containsExactly("Bandeja paisa", "Limonada");   // el más lento primero
        assertThat(platos.get(0).minutosPromedio()).isEqualByComparingTo("25.0000");
        assertThat(platos.get(0).veces()).isEqualTo(2);
        assertThat(platos.get(1).minutosPromedio())
                .as("la línea sin marca de cocina no cuenta: promediarla como cero mentiría")
                .isEqualByComparingTo("4.0000");
        assertThat(platos.get(1).veces()).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 3: una ferretería no tiene mesas que rotar")
    void laFerreteriaNoVeMetricasDeRestaurante() {
        assertThatThrownBy(() -> enContexto(ferreteria, duenio, "VENTA_DIRECTA", VER,
                () -> metricas.mesas(LocalDate.now().minusDays(1), LocalDate.now())))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> enContexto(ferreteria, duenio, "VENTA_DIRECTA", VER,
                () -> metricas.preparacion(LocalDate.now().minusDays(1), LocalDate.now())))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("HU-134 criterio 4: el reporte por mesa sale por código y zona, no por id")
    void reportePorMesaSalePorCodigoYZona() {
        UUID restaurante = UUID.randomUUID();
        UUID mesa1 = UUID.randomUUID();
        UUID mesa2 = UUID.randomUUID();
        mesa(restaurante, mesa1, "T1", "Terraza");
        mesa(restaurante, mesa2, "S1", "Salón");
        pedido(restaurante, mesa1, 2, 40, new Plato("Bandeja", "60000", 15));
        pedido(restaurante, mesa1, 3, 80, new Plato("Churrasco", "90000", 20));
        pedido(restaurante, mesa2, 1, 30, new Plato("Sopa", "30000", 10));

        List<RotacionDeMesa> porMesa = enContexto(restaurante, duenio, "COMANDA", VER,
                () -> metricas.porMesa(LocalDate.now().minusDays(1), LocalDate.now()));

        assertThat(porMesa).extracting(RotacionDeMesa::codigo).containsExactlyInAnyOrder("T1", "S1");
        assertThat(porMesa).filteredOn(m -> m.codigo().equals("T1"))
                .extracting(RotacionDeMesa::zona).containsExactly("Terraza");
        assertThat(porMesa).filteredOn(m -> m.codigo().equals("T1"))
                .extracting(RotacionDeMesa::comandas).containsExactly(2);
        assertThat(porMesa).filteredOn(m -> m.codigo().equals("S1"))
                .extracting(RotacionDeMesa::zona).containsExactly("Salón");
    }

    @Test
    @DisplayName("HU-134 criterio 5: una mesa eliminada sigue saliendo en el reporte con su código")
    void mesaEliminadaSigueEnElReporteConSuCodigo() {
        UUID restaurante = UUID.randomUUID();
        UUID mesaId = UUID.randomUUID();
        mesa(restaurante, mesaId, "T9", "Terraza");
        pedido(restaurante, mesaId, 2, 40, new Plato("Bandeja", "60000", 15));

        Map<String, Object> eliminar = new LinkedHashMap<>();
        eliminar.put("negocio_id", restaurante.toString());
        eliminar.put("mesa_id", mesaId.toString());
        eliminar.put("zona_id", null);
        eliminar.put("zona_nombre", null);
        eliminar.put("codigo", "T9");
        eliminar.put("nombre", "Mesa T9");
        eliminar.put("capacidad", 4);
        eliminar.put("activa", false);
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("mesa_eliminada");
            mesas.recibir(
                    MessageBuilder.withBody(json.writeValueAsBytes(eliminar)).andProperties(props).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<RotacionDeMesa> porMesa = enContexto(restaurante, duenio, "COMANDA", VER,
                () -> metricas.porMesa(LocalDate.now().minusDays(1), LocalDate.now()));

        assertThat(porMesa).extracting(RotacionDeMesa::codigo).containsExactly("T9");
    }

    @Test
    @DisplayName("Un restaurante no ve las mesas del de al lado")
    void aislamientoEntreRestaurantes() {
        UUID mio = UUID.randomUUID();
        UUID delVecino = UUID.randomUUID();
        pedido(delVecino, UUID.randomUUID(), 4, 60, new Plato("Pizza", "70000", 18));

        MesasDelPeriodo mias = enContexto(mio, duenio, "COMANDA", VER,
                () -> metricas.mesas(LocalDate.now().minusDays(1), LocalDate.now()));

        assertThat(mias.comandas()).isZero();
        assertThat(mias.ticketPorComensal()).isEqualByComparingTo("0");
        assertThat(enContexto(delVecino, duenio, "COMANDA", VER,
                () -> metricas.mesas(LocalDate.now().minusDays(1), LocalDate.now())).comandas())
                .isEqualTo(1);
    }
}
