package com.regenta.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.regenta.clientes.BaseDeClientes;
import com.regenta.clientes.infra.ConsumidorDeCierres;

/** HU-023. Metricas del cliente alimentadas por eventos. */
class ProyeccionDeMetricasTest extends BaseDeClientes {

    private static final Set<String> DE_VENDEDOR = Set.of("CLIENTES_CLIENTE_VER",
            "CLIENTES_CLIENTE_CREAR");

    @Autowired
    private ConsumidorDeCierres consumidor;

    @Autowired
    private GestionDeClientes clientes;

    @Autowired
    private ProyeccionDeMetricas metricas;

    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID clienteEn(UUID negocio, String nombre) {
        return enContexto(negocio, usuario, DE_VENDEDOR, () -> clientes.crear(new SolicitudDeCliente(
                "NATURAL", "CC", nombre.hashCode() + "", null, nombre, "X", null, null, null, null,
                null, null))).id();
    }

    private void entregar(String tipoEvento, UUID mensajeId, Map<String, Object> payload) {
        try {
            byte[] cuerpo = json.writeValueAsBytes(payload);
            MessageProperties props = new MessageProperties();
            props.setMessageId(mensajeId.toString());
            props.setReceivedRoutingKey(tipoEvento);
            Message mensaje = MessageBuilder.withBody(cuerpo).andProperties(props).build();
            consumidor.recibir(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> compra(UUID negocio, UUID cliente, String monto,
            String ocurrido) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", negocio.toString());
        if (cliente != null) {
            p.put("cliente_id", cliente.toString());
        }
        p.put("total", monto);
        p.put("ocurrido_en", ocurrido);
        return p;
    }

    private String metrica(UUID negocio, UUID cliente, String columna) {
        return comoElServicio(negocio, "select " + columna + " from cliente_metricas where cliente_id = '"
                + cliente + "'").stream().findFirst().orElse(null);
    }

    @Test
    @DisplayName("Criterio 1: venta_completada suma el documento y el monto del cliente")
    void ventaCompletadaSuma() {
        UUID cliente = clienteEn(negocioA, "Ana");

        entregar("venta_completada", UUID.randomUUID(),
                compra(negocioA, cliente, "150000.00", "2026-09-01T10:00:00Z"));

        assertThat(metrica(negocioA, cliente, "total_documentos")).isEqualTo("1");
        assertThat(metrica(negocioA, cliente, "monto_total")).isEqualTo("150000.00");
        assertThat(metrica(negocioA, cliente, "primera_compra_en")).isNotNull();
        assertThat(metrica(negocioA, cliente, "ultima_compra_en")).isNotNull();
    }

    @Test
    @DisplayName("Criterio 2: el mismo evento entregado dos veces no duplica las metricas")
    void eventoDuplicadoNoSuma() {
        UUID cliente = clienteEn(negocioA, "Ben");
        UUID mensajeId = UUID.randomUUID();
        Map<String, Object> payload = compra(negocioA, cliente, "80000.00", "2026-09-02T12:00:00Z");

        entregar("venta_completada", mensajeId, payload);
        entregar("venta_completada", mensajeId, payload);

        assertThat(metrica(negocioA, cliente, "total_documentos")).isEqualTo("1");
        assertThat(metrica(negocioA, cliente, "monto_total")).isEqualTo("80000.00");
    }

    @Test
    @DisplayName("Criterio 3: estancia_finalizada y pedido_completado alimentan la misma proyeccion")
    void losTresPatronesAlimentanLaMisma() {
        UUID cliente = clienteEn(negocioA, "Cid");

        entregar("estancia_finalizada", UUID.randomUUID(),
                compra(negocioA, cliente, "100000.00", "2026-09-03T09:00:00Z"));
        entregar("pedido_completado", UUID.randomUUID(),
                compra(negocioA, cliente, "50000.00", "2026-09-04T09:00:00Z"));

        assertThat(metrica(negocioA, cliente, "total_documentos")).isEqualTo("2");
        assertThat(metrica(negocioA, cliente, "monto_total")).isEqualTo("150000.00");
        assertThat(metrica(negocioA, cliente, "ticket_promedio")).isEqualTo("75000.00");
    }

    @Test
    @DisplayName("Criterio 4: venta_anulada ajusta las metricas hacia abajo")
    void ventaAnuladaAjustaAbajo() {
        UUID cliente = clienteEn(negocioA, "Dan");
        entregar("venta_completada", UUID.randomUUID(),
                compra(negocioA, cliente, "120000.00", "2026-09-05T09:00:00Z"));
        entregar("venta_completada", UUID.randomUUID(),
                compra(negocioA, cliente, "80000.00", "2026-09-06T09:00:00Z"));

        entregar("venta_anulada", UUID.randomUUID(),
                compra(negocioA, cliente, "80000.00", "2026-09-07T09:00:00Z"));

        assertThat(metrica(negocioA, cliente, "total_documentos")).isEqualTo("1");
        assertThat(metrica(negocioA, cliente, "monto_total")).isEqualTo("120000.00");
    }

    @Test
    @DisplayName("Un evento de un negocio no toca las metricas de otro")
    void aisladoPorNegocio() {
        UUID clienteA = clienteEn(negocioA, "Eva");
        clienteEn(negocioB, "Eve");

        entregar("venta_completada", UUID.randomUUID(),
                compra(negocioA, clienteA, "90000.00", "2026-09-08T09:00:00Z"));

        assertThat(comoElServicio(negocioB, "select count(*) from cliente_metricas"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Una venta sin cliente (consumidor final) no crea fila de metricas")
    void sinClienteNoCreaFila() {
        entregar("venta_completada", UUID.randomUUID(),
                compra(negocioA, null, "40000.00", "2026-09-09T09:00:00Z"));

        assertThat(comoElServicio(negocioA, "select count(*) from cliente_metricas"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("El endpoint de metricas devuelve ceros mientras el cliente no ha comprado")
    void verSinComprasDaCeros() {
        UUID cliente = clienteEn(negocioA, "Fio");

        MetricasDelCliente vista = enContexto(negocioA, usuario, DE_VENDEDOR,
                () -> metricas.ver(cliente));

        assertThat(vista.totalDocumentos()).isZero();
        assertThat(vista.montoTotal().signum()).isZero();

        entregar("venta_completada", UUID.randomUUID(),
                compra(negocioA, cliente, "25000.00", "2026-09-10T09:00:00Z"));

        MetricasDelCliente conCompra = enContexto(negocioA, usuario, DE_VENDEDOR,
                () -> metricas.ver(cliente));
        assertThat(conCompra.totalDocumentos()).isEqualTo(1);
        assertThat(conCompra.montoTotal()).isEqualByComparingTo("25000.00");
    }
}
