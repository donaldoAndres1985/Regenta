package com.regenta.alertas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.alertas.BaseDeAlertas;
import com.regenta.alertas.aplicacion.ConsultaDeInventario.LotePorVencer;
import com.regenta.alertas.infra.ConsultaDeInventarioStub;
import com.regenta.alertas.infra.ConsumidorDeInventario;

/** HU-093. Alertas de inventario: stock bajo y lotes por vencer. */
class AlertasDeInventarioTest extends BaseDeAlertas {

    private static final Set<String> ADMIN =
            Set.of("ALERTAS_ALERTA_VER", "ALERTAS_ALERTA_EDITAR");

    @Autowired
    private GestionDeReglas reglas;
    @Autowired
    private GestionDeVigilancia vigilancia;
    @Autowired
    private ConsumidorDeInventario consumidor;
    @Autowired
    private ConsultaDeInventarioStub inventario;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        inventario.reiniciar();
    }

    private void reglaStock(String nombre) {
        enContexto(negocioA, admin, ADMIN, () -> reglas.crear(new SolicitudDeRegla(
                "STOCK_MINIMO", nombre, null, Map.of(), "ALTA", List.of("IN_APP"),
                List.of(), List.of(), "INMEDIATA", null, 0)));
    }

    private void reglaVencimiento(String nombre, int dias) {
        enContexto(negocioA, admin, ADMIN, () -> reglas.crear(new SolicitudDeRegla(
                "VENCIMIENTO_LOTE", nombre, null,
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", dias), "ALTA",
                List.of("IN_APP"), List.of(), List.of(), "INMEDIATA", null, 24)));
    }

    private Message mensaje(String clave, Map<String, Object> payload) {
        try {
            byte[] cuerpo = json.writeValueAsBytes(payload);
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(clave);
            return MessageBuilder.withBody(cuerpo).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message mensajeConId(String clave, String id, Map<String, Object> payload) {
        Message base = mensaje(clave, payload);
        base.getMessageProperties().setMessageId(id);
        return base;
    }

    @Test
    @DisplayName("Criterio 1: stock_bajo_minimo genera la alerta de stock")
    void stockBajoGeneraAlerta() {
        reglaStock("Reponer");
        UUID producto = UUID.randomUUID();

        consumidor.recibir(mensaje("stock_bajo_minimo", Map.of(
                "negocio_id", negocioA.toString(),
                "producto_id", producto.toString(),
                "producto_nombre", "Guantes M",
                "existencia", 3, "minimo", 10,
                "bodega_id", UUID.randomUUID().toString(),
                "bodega_nombre", "Central")));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where tipo_codigo = 'STOCK_MINIMO' "
                        + "and entidad_id = '" + producto + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select ruta_app from alertas where entidad_id = '" + producto + "'"))
                .containsExactly("/inventario/productos/" + producto);
        assertThat(comoElServicio(negocioA,
                "select mensaje from alertas where entidad_id = '" + producto + "'").get(0))
                .contains("Guantes M").contains("3").contains("10");
    }

    @Test
    @DisplayName("Criterio 3: la alerta de stock lleva al producto y a la sugerencia de compra")
    void alertaLlevaAProductoYSugerencia() {
        reglaStock("Reponer");
        UUID producto = UUID.randomUUID();

        consumidor.recibir(mensaje("stock_bajo_minimo", Map.of(
                "negocio_id", negocioA.toString(), "producto_id", producto.toString(),
                "producto_nombre", "Jeringa", "existencia", 1, "minimo", 20)));

        assertThat(comoElServicio(negocioA,
                "select ruta_app from alertas where entidad_id = '" + producto + "'"))
                .containsExactly("/inventario/productos/" + producto);
        assertThat(comoElServicio(negocioA,
                "select datos::text from alertas where entidad_id = '" + producto + "'").get(0))
                .contains("/compras/sugerencias?producto=" + producto);
    }

    @Test
    @DisplayName("Criterio 4: cuando el stock se normaliza, la alerta se resuelve sola")
    void stockNormalizadoResuelve() {
        reglaStock("Reponer");
        UUID producto = UUID.randomUUID();
        consumidor.recibir(mensaje("stock_bajo_minimo", Map.of(
                "negocio_id", negocioA.toString(), "producto_id", producto.toString(),
                "producto_nombre", "Gasas", "existencia", 2, "minimo", 15)));
        assertThat(comoElServicio(negocioA,
                "select estado from alertas where entidad_id = '" + producto + "'"))
                .containsExactly("NUEVA");

        consumidor.recibir(mensaje("stock_normalizado", Map.of(
                "negocio_id", negocioA.toString(), "producto_id", producto.toString())));

        assertThat(comoElServicio(negocioA,
                "select estado from alertas where entidad_id = '" + producto + "'"))
                .containsExactly("RESUELTA");
    }

    @Test
    @DisplayName("Criterio 2: el barrido publica un lote_por_vencer por lote en la ventana")
    void barridoPublicaLotePorVencer() {
        reglaVencimiento("30d", 30);
        UUID lote1 = UUID.randomUUID();
        UUID lote2 = UUID.randomUUID();
        UUID lejano = UUID.randomUUID();
        inventario.cargar(negocioA, List.of(
                new LotePorVencer(lote1, UUID.randomUUID(), "Amoxicilina", "L-1", 10,
                        LocalDate.now().plusDays(10), null),
                new LotePorVencer(lote2, UUID.randomUUID(), "Ibuprofeno", "L-2", 25,
                        LocalDate.now().plusDays(25), null),
                new LotePorVencer(lejano, UUID.randomUUID(), "Suero", "L-3", 200,
                        LocalDate.now().plusDays(200), null)));

        int publicados = enContexto(negocioA, admin, ADMIN,
                () -> vigilancia.barrerVencimientos());

        assertThat(publicados).isEqualTo(2);
        assertThat(comoElServicio(negocioA,
                "select count(*) from outbox_eventos where tipo_evento = 'lote_por_vencer' "
                        + "and agregado_id in ('" + lote1 + "','" + lote2 + "')"))
                .containsExactly("2");
        assertThat(comoElServicio(negocioA,
                "select count(*) from outbox_eventos where tipo_evento = 'lote_por_vencer' "
                        + "and agregado_id = '" + lejano + "'"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Criterio 2: el evento lote_por_vencer genera la alerta de vencimiento")
    void lotePorVencerGeneraAlerta() {
        reglaVencimiento("30d", 30);
        UUID lote = UUID.randomUUID();

        consumidor.recibir(mensaje("lote_por_vencer", Map.of(
                "negocio_id", negocioA.toString(),
                "lote_id", lote.toString(),
                "producto_nombre", "Acetaminofén",
                "lote_codigo", "L-2411",
                "dias_para_vencer", 20,
                "fecha_vencimiento", "2027-03-20")));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where tipo_codigo = 'VENCIMIENTO_LOTE' "
                        + "and entidad_id = '" + lote + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select mensaje from alertas where entidad_id = '" + lote + "'").get(0))
                .contains("L-2411").contains("20 días");
    }

    @Test
    @DisplayName("Un lote_por_vencer lejos de la ventana de la regla no genera alerta")
    void lotePorVencerFueraDeRango() {
        reglaVencimiento("7d", 7);
        UUID lote = UUID.randomUUID();

        consumidor.recibir(mensaje("lote_por_vencer", Map.of(
                "negocio_id", negocioA.toString(), "lote_id", lote.toString(),
                "producto_nombre", "X", "lote_codigo", "L", "dias_para_vencer", 40)));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + lote + "'"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("El mismo evento repetido no genera una segunda alerta (Inbox)")
    void eventoRepetidoNoDuplica() {
        reglaStock("Reponer");
        UUID producto = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "negocio_id", negocioA.toString(), "producto_id", producto.toString(),
                "producto_nombre", "Alcohol", "existencia", 0, "minimo", 5);

        consumidor.recibir(mensajeConId("stock_bajo_minimo", id, payload));
        consumidor.recibir(mensajeConId("stock_bajo_minimo", id, payload));

        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + producto + "'"))
                .containsExactly("1");
    }
}
