package com.regenta.caja.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import com.regenta.caja.BaseDeCaja;
import com.regenta.caja.infra.ConsumidorDeCobros;

/** HU-060. Los cobros de los tres patrones entran a la caja. */
class MovimientosDeCajaTest extends BaseDeCaja {

    private static final Set<String> CAJERO = Set.of("CAJA_TURNO_VER", "CAJA_TURNO_CREAR",
            "CAJA_TURNO_EDITAR", "CAJA_MOVIMIENTO_VER");

    @Autowired
    private ConsumidorDeCobros consumidor;
    @Autowired
    private GestionDeCajas cajas;
    @Autowired
    private GestionDeSesionesDeCaja sesiones;
    @Autowired
    private RegistroDeMovimientos movimientos;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();

    private int seq = 0;

    private SesionDelNegocio sesionAbierta(UUID negocio, String base) {
        String codigo = "C" + (++seq) + "-" + Integer.toHexString(negocio.hashCode());
        UUID caja = enContexto(negocio, cajero, CAJERO,
                () -> cajas.crear(new SolicitudDeCaja(codigo, "Caja", null, null))).id();
        return enContexto(negocio, cajero, CAJERO,
                () -> sesiones.abrir(new SolicitudDeApertura(caja, new BigDecimal(base))));
    }

    private Message evento(String clave, String id, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(id);
            props.setReceivedRoutingKey(clave);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> cobro(UUID sesionId, UUID origenId, Object... pagos) {
        return Map.of(
                "negocio_id", negocioA.toString(),
                "sesion_caja_id", sesionId.toString(),
                "origen_id", origenId.toString(),
                "numero", "DOC-1",
                "usuario_id", cajero.toString(),
                "pagos", List.of(pagos));
    }

    private Map<String, Object> pago(String metodo, String monto) {
        return Map.of("metodo", metodo, "monto", monto);
    }

    @Test
    @DisplayName("Criterio 1: venta_completada registra el movimiento en la sesión indicada")
    void ventaEntraALaCaja() {
        SesionDelNegocio s = sesionAbierta(negocioA, "100000");
        UUID venta = UUID.randomUUID();

        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(),
                cobro(s.id(), venta, pago("EFECTIVO", "50000"))));

        List<MovimientoDelNegocio> movs = enContexto(negocioA, cajero, CAJERO,
                () -> movimientos.deLaSesion(s.id()));
        assertThat(movs).filteredOn(m -> m.tipo().equals("VENTA")).singleElement()
                .satisfies(m -> {
                    assertThat(m.metodoPago()).isEqualTo("EFECTIVO");
                    assertThat(m.monto()).isEqualByComparingTo("50000");
                    assertThat(m.origenId()).isEqualTo(venta);
                    assertThat(m.signo()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Criterio 2: pedido_completado entra con origen COMANDA")
    void comandaEntraALaCaja() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        consumidor.recibir(evento("pedido_completado", UUID.randomUUID().toString(),
                cobro(s.id(), UUID.randomUUID(), pago("TARJETA_CREDITO", "80000"))));

        assertThat(comoElServicio(negocioA,
                "select tipo from movimientos_caja where sesion_id = '" + s.id() + "'"))
                .containsExactly("COMANDA");
    }

    @Test
    @DisplayName("Criterio 3: el anticipo de una reserva entra con origen RESERVA")
    void reservaEntraALaCaja() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        consumidor.recibir(evento("anticipo_reserva_cobrado", UUID.randomUUID().toString(),
                cobro(s.id(), UUID.randomUUID(), pago("TRANSFERENCIA", "120000"))));

        assertThat(comoElServicio(negocioA,
                "select tipo from movimientos_caja where sesion_id = '" + s.id() + "'"))
                .containsExactly("RESERVA");
    }

    @Test
    @DisplayName("Criterio 4: el mismo evento dos veces no registra el movimiento dos veces (Inbox)")
    void eventoRepetidoNoDuplica() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        UUID venta = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        Map<String, Object> payload = cobro(s.id(), venta, pago("EFECTIVO", "10000"));

        consumidor.recibir(evento("venta_completada", id, payload));
        consumidor.recibir(evento("venta_completada", id, payload));

        assertThat(comoElServicio(negocioA,
                "select count(*) from movimientos_caja where sesion_id = '" + s.id() + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 4: el mismo cobro con otro message-id tampoco duplica (uq_mov_caja_idem)")
    void mismoCobroOtroMensajeNoDuplica() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        UUID venta = UUID.randomUUID();
        Map<String, Object> payload = cobro(s.id(), venta, pago("EFECTIVO", "10000"));

        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(), payload));
        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(), payload));

        assertThat(comoElServicio(negocioA,
                "select count(*) from movimientos_caja where sesion_id = '" + s.id() + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Un cobro mixto genera un movimiento por método de pago")
    void cobroMixto() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(),
                cobro(s.id(), UUID.randomUUID(),
                        pago("EFECTIVO", "30000"), pago("TARJETA_CREDITO", "20000"))));

        assertThat(comoElServicio(negocioA,
                "select count(*) from movimientos_caja where sesion_id = '" + s.id() + "'"))
                .containsExactly("2");
    }

    @Test
    @DisplayName("Un cobro sin sesión de caja no genera movimiento")
    void cobroSinSesion() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(), Map.of(
                "negocio_id", negocioA.toString(),
                "origen_id", UUID.randomUUID().toString(),
                "pagos", List.of(pago("EFECTIVO", "5000")))));

        assertThat(comoElServicio(negocioA, "select count(*) from movimientos_caja"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("El efectivo cobrado se suma a lo esperado del arqueo (integra con HU-059)")
    void efectivoSumaAlArqueo() {
        SesionDelNegocio s = sesionAbierta(negocioA, "100000");
        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(),
                cobro(s.id(), UUID.randomUUID(), pago("EFECTIVO", "50000"))));
        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(),
                cobro(s.id(), UUID.randomUUID(), pago("TARJETA_CREDITO", "70000"))));

        SesionDelNegocio cerrada = enContexto(negocioA, cajero, CAJERO,
                () -> sesiones.cerrar(s.id(), new SolicitudDeCierre(new BigDecimal("150000"), null)));

        assertThat(cerrada.montoEsperado()).isEqualByComparingTo("150000"); // 100k base + 50k efectivo
        assertThat(cerrada.estado()).isEqualTo("CUADRADA");
    }

    @Test
    @DisplayName("Los movimientos de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        SesionDelNegocio s = sesionAbierta(negocioA, "0");
        consumidor.recibir(evento("venta_completada", UUID.randomUUID().toString(),
                cobro(s.id(), UUID.randomUUID(), pago("EFECTIVO", "9000"))));

        assertThat(comoElServicio(negocioB, "select count(*) from movimientos_caja"))
                .containsExactly("0");
    }
}
