package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.ventas.BaseDeVentas;
import com.regenta.ventas.domain.MotivoDevolucion;

/** HU-042. Devoluciones totales y parciales. */
class GestionDeDevolucionesTest extends BaseDeVentas {

    private static final Set<String> SETUP = Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER",
            "VENTAS_VENTA_CONFIRMAR", "VENTAS_DEVOLUCION_REGISTRAR");

    @Autowired
    private GestionDeVentas ventas;

    @Autowired
    private GestionDeDevoluciones devoluciones;

    @Autowired
    private SagaDeConfirmacionDeVenta saga;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private final UUID bodegaDevoluciones = UUID.randomUUID();

    private UUID venta;

    @BeforeEach
    void preparar() {
        venta = enContexto(negocio, usuario, SETUP, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                    .id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Prod", "UND",
                    new BigDecimal("10"), new BigDecimal("100"), BigDecimal.ZERO, "IVA0",
                    BigDecimal.ZERO, new BigDecimal("40")));
            ventas.confirmar(id);
            UUID corr = UUID.fromString(comoElServicio(negocio,
                    "select correlacion_id from sagas where agregado_id = '" + id + "'").get(0));
            saga.alStockReservado(corr);
            return id;
        });
    }

    private void devolver(String cantidad, MotivoDevolucion motivo, boolean reintegra) {
        enContexto(negocio, usuario, SETUP, () -> devoluciones.registrar(venta,
                new SolicitudDeDevolucion(motivo, "detalle", reintegra, bodegaDevoluciones,
                        List.of(new LineaDevuelta((short) 1, new BigDecimal(cantidad))))));
    }

    private String estadoVenta() {
        return comoElServicio(negocio, "select estado from ventas where id = '" + venta + "'").get(0);
    }

    private String cantidadDevuelta() {
        return comoElServicio(negocio,
                "select cantidad_devuelta from venta_lineas where venta_id = '" + venta + "'").get(0);
    }

    private String payloadDe(String tipoEvento) {
        var filas = comoElServicio(negocio, "select payload::text from outbox_eventos where "
                + "negocio_id = '" + negocio + "' and tipo_evento = '" + tipoEvento + "'");
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Test
    @DisplayName("Criterio 1: devolver 3 de 10 deja la venta DEVUELTA_PARCIAL y la línea con 3 devueltas")
    void devolucionParcial() {
        devolver("3", MotivoDevolucion.ARREPENTIMIENTO, true);

        assertThat(estadoVenta()).isEqualTo("DEVUELTA_PARCIAL");
        assertThat(new BigDecimal(cantidadDevuelta())).isEqualByComparingTo("3");
        assertThat(comoElServicio(negocio, "select tipo from devoluciones")).containsExactly("PARCIAL");
    }

    @Test
    @DisplayName("Criterio 2: devolver más de lo que queda por devolver se rechaza")
    void noSeDevuelveDeMas() {
        devolver("3", MotivoDevolucion.ARREPENTIMIENTO, true);

        assertThatThrownBy(() -> devolver("8", MotivoDevolucion.ARREPENTIMIENTO, true))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("devolv");
        assertThat(new BigDecimal(cantidadDevuelta())).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("Criterio 3: una devolución con reintegro indica la bodega de destino en el evento")
    void devolucionConReintegro() {
        devolver("4", MotivoDevolucion.ERROR_DESPACHO, true);

        String payload = payloadDe("devolucion_registrada");
        assertThat(payload).contains("\"reintegra_stock\": true").contains(bodegaDevoluciones.toString());
    }

    @Test
    @DisplayName("Criterio 4: una devolución de producto defectuoso sin reintegro no sube stock y deja el motivo")
    void devolucionSinReintegro() {
        devolver("2", MotivoDevolucion.DEFECTUOSO, false);

        String payload = payloadDe("devolucion_registrada");
        assertThat(payload).contains("\"reintegra_stock\": false").contains("DEFECTUOSO");
        assertThat(comoElServicio(negocio, "select reintegra_stock from devoluciones"))
                .containsExactly("f");
        assertThat(new BigDecimal(cantidadDevuelta())).isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Criterio 5: una devolución sobre una venta facturada dispara la nota crédito")
    void devolucionSobreVentaFacturada() {
        ejecutarComoElServicio(negocio,
                "update ventas set estado_factura = 'EMITIDA' where id = '" + venta + "'");

        devolver("1", MotivoDevolucion.GARANTIA, true);

        assertThat(comoElServicio(negocio, "select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'nota_credito_requerida'"))
                .containsExactly("nota_credito_requerida");
    }

    @Test
    @DisplayName("Devolver todo lo vendido deja la venta DEVUELTA")
    void devolucionTotal() {
        devolver("10", MotivoDevolucion.ARREPENTIMIENTO, true);
        assertThat(estadoVenta()).isEqualTo("DEVUELTA");
        assertThat(comoElServicio(negocio, "select tipo from devoluciones")).containsExactly("TOTAL");
    }

    @Test
    @DisplayName("Las devoluciones de otro negocio no se ven")
    void aisladoPorNegocio() {
        devolver("1", MotivoDevolucion.OTRO, true);
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from devoluciones"))
                .containsExactly("0");
    }
}
