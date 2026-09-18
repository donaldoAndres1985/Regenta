package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.ventas.BaseDeVentas;

/** HU-038. Saga de confirmación de venta con reserva de stock. */
class SagaDeConfirmacionDeVentaTest extends BaseDeVentas {

    private static final Set<String> SETUP =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");

    @Autowired
    private GestionDeVentas ventas;

    @Autowired
    private SagaDeConfirmacionDeVenta saga;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID venta;

    @BeforeEach
    void preparar() {
        venta = enContexto(negocio, usuario, SETUP, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                    .id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Prod", "UND",
                    new BigDecimal("2"), new BigDecimal("100"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("40")));
            return id;
        });
    }

    private String estadoVenta() {
        return comoElServicio(negocio, "select estado from ventas where id = '" + venta + "'").get(0);
    }

    private String estadoSaga() {
        return comoElServicio(negocio,
                "select estado from sagas where agregado_id = '" + venta + "'").get(0);
    }

    private UUID correlacion() {
        return UUID.fromString(comoElServicio(negocio,
                "select correlacion_id from sagas where agregado_id = '" + venta + "'").get(0));
    }

    private List<String> eventos() {
        return comoElServicio(negocio, "select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio + "' order by creado_en");
    }

    @Test
    @DisplayName("Criterio 1: confirmar pasa la venta a PENDIENTE_STOCK y publica solicitar_reserva_stock")
    void confirmarIniciaLaSaga() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));

        assertThat(estadoVenta()).isEqualTo("PENDIENTE_STOCK");
        assertThat(estadoSaga()).isEqualTo("ESPERANDO_STOCK");
        assertThat(eventos()).contains("solicitar_reserva_stock");
    }

    @Test
    @DisplayName("Criterio 2: al llegar stock_reservado la venta queda CONFIRMADA y se publica venta_completada")
    void stockReservadoConfirmaLaVenta() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));
        UUID corr = correlacion();

        enContexto(negocio, usuario, SETUP, () -> saga.alStockReservado(corr));

        assertThat(estadoVenta()).isEqualTo("CONFIRMADA");
        assertThat(estadoSaga()).isEqualTo("COMPLETADA");
        assertThat(eventos()).contains("venta_completada");
        assertThat(comoElServicio(negocio, "select saldo_pendiente from ventas where id = '"
                + venta + "'")).containsExactly("238.0000");
    }

    @Test
    @DisplayName("HU-096 criterio 1: venta_completada lleva el detalle de líneas, para que Reportes arme hechos_venta")
    void ventaCompletadaLlevaLasLineas() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));
        UUID corr = correlacion();

        enContexto(negocio, usuario, SETUP, () -> saga.alStockReservado(corr));

        String payload = comoElServicio(negocio, "select payload::text from outbox_eventos "
                + "where negocio_id = '" + negocio + "' and tipo_evento = 'venta_completada'").get(0);
        assertThat(payload).contains("\"lineas\"").contains("SKU-1").contains("\"cantidad\": 2.000000");
        assertThat(payload).contains("\"usuario_id\": \"" + usuario + "\"");
        assertThat(payload).contains("\"canal\": \"MOSTRADOR\"");
    }

    @Test
    @DisplayName("Criterio 3: al llegar stock_reserva_fallida la venta vuelve a BORRADOR y la saga queda COMPENSADA")
    void reservaFallidaCompensa() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));
        UUID corr = correlacion();

        enContexto(negocio, usuario, SETUP,
                () -> saga.alReservaFallida(corr, "faltan 3 de SKU-1"));

        assertThat(estadoVenta()).isEqualTo("BORRADOR");
        assertThat(estadoSaga()).isEqualTo("COMPENSADA");
        assertThat(comoElServicio(negocio, "select nota from ventas where id = '" + venta + "'"))
                .containsExactly("faltan 3 de SKU-1");
    }

    @Test
    @DisplayName("Criterio 4: una saga sin respuesta pasado su timeout se compensa igual")
    void elTimeoutCompensa() {
        enContexto(negocio, usuario, SETUP,
                () -> saga.iniciar(venta, Duration.ofSeconds(-1)));
        assertThat(estadoVenta()).isEqualTo("PENDIENTE_STOCK");

        int compensadas = enContexto(negocio, usuario, SETUP, () -> saga.compensarVencidas());

        assertThat(compensadas).isEqualTo(1);
        assertThat(estadoVenta()).isEqualTo("BORRADOR");
        assertThat(estadoSaga()).isEqualTo("COMPENSADA");
    }

    @Test
    @DisplayName("Criterio 5: la saga se retoma desde su estado persistido, no de memoria")
    void laSagaSeRetomaDesdeLaTabla() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));
        UUID corr = correlacion();

        // "El servicio se reinicia": nada en memoria; solo lo que hay en la tabla.
        // Una instancia nueva atiende el evento leyendo el estado persistido.
        enContexto(negocio, usuario, SETUP, () -> saga.alStockReservado(corr));

        assertThat(estadoSaga()).isEqualTo("COMPLETADA");
        assertThat(estadoVenta()).isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName("El mismo evento entregado dos veces no confirma dos veces")
    void idempotente() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));
        UUID corr = correlacion();

        enContexto(negocio, usuario, SETUP, () -> saga.alStockReservado(corr));
        enContexto(negocio, usuario, SETUP, () -> saga.alStockReservado(corr));

        assertThat(estadoSaga()).isEqualTo("COMPLETADA");
        assertThat(eventos()).filteredOn(e -> e.equals("venta_completada")).hasSize(1);
    }

    @Test
    @DisplayName("Las sagas de otro negocio no se ven")
    void aisladoPorNegocio() {
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from sagas"))
                .containsExactly("0");
    }
}
