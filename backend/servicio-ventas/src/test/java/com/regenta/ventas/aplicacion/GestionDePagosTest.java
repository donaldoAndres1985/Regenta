package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.ventas.BaseDeVentas;
import com.regenta.ventas.domain.MetodoDePago;

/** HU-039. Registrar el pago de una venta, incluso mixto. */
class GestionDePagosTest extends BaseDeVentas {

    private static final Set<String> SETUP = Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER",
            "VENTAS_VENTA_CONFIRMAR", "VENTAS_PAGO_REGISTRAR");

    @Autowired
    private GestionDeVentas ventas;

    @Autowired
    private GestionDePagos pagos;

    @Autowired
    private SagaDeConfirmacionDeVenta saga;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    /** Una venta CONFIRMADA por 238 (2 x 100 + 19%). */
    private UUID ventaConfirmada(UUID clienteId) {
        return enContexto(negocio, usuario, SETUP, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, clienteId, null, "MOSTRADOR"))
                    .id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Prod", "UND",
                    new BigDecimal("2"), new BigDecimal("100"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("40")));
            ventas.confirmar(id);
            UUID corr = UUID.fromString(comoElServicio(negocio,
                    "select correlacion_id from sagas where agregado_id = '" + id + "'").get(0));
            saga.alStockReservado(corr);
            return id;
        });
    }

    private UUID venta;

    @BeforeEach
    void preparar() {
        venta = ventaConfirmada(null);
    }

    private ResultadoDePago pagar(MetodoDePago metodo, String monto, String recibido, String ref,
            String franquicia, Integer diasCredito) {
        return enContexto(negocio, usuario, SETUP, () -> pagos.registrarPago(venta,
                new SolicitudDePago(metodo, monto == null ? null : new BigDecimal(monto),
                        recibido == null ? null : new BigDecimal(recibido), ref, franquicia,
                        diasCredito)));
    }

    private String saldo() {
        return comoElServicio(negocio, "select saldo_pendiente from ventas where id = '" + venta
                + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: con pagos por menos del total la venta no se puede cerrar")
    void noSeCierraConMenosDelTotal() {
        pagar(MetodoDePago.EFECTIVO, "100", "100", null, null, null);

        assertThat(new BigDecimal(saldo())).isEqualByComparingTo("138");
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> pagos.cerrarVenta(venta)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("138");
    }

    @Test
    @DisplayName("Criterio 2: un pago en efectivo mayor al total calcula el cambio")
    void efectivoMayorAlTotalDaCambio() {
        ResultadoDePago r = pagar(MetodoDePago.EFECTIVO, null, "300", null, null, null);

        assertThat(r.montoAplicado()).isEqualByComparingTo("238");
        assertThat(r.cambio()).isEqualByComparingTo("62");
        assertThat(new BigDecimal(saldo())).isEqualByComparingTo("0");
        assertThat(comoElServicio(negocio, "select cambio from pagos_venta"))
                .containsExactly("62.0000");
        enContexto(negocio, usuario, SETUP, () -> pagos.cerrarVenta(venta));
    }

    @Test
    @DisplayName("Criterio 3: dos medios de pago que suman el total dejan la venta pagada")
    void dosMediosSumanElTotal() {
        pagar(MetodoDePago.EFECTIVO, "138", "138", null, null, null);
        ResultadoDePago r = pagar(MetodoDePago.TARJETA_DEBITO, "100", null, "0000", "MASTERCARD",
                null);

        assertThat(r.cubierta()).isTrue();
        assertThat(new BigDecimal(saldo())).isEqualByComparingTo("0");
        assertThat(comoElServicio(negocio, "select forma_pago from ventas where id = '" + venta
                + "'")).containsExactly("MIXTO");
        enContexto(negocio, usuario, SETUP, () -> pagos.cerrarVenta(venta));
    }

    @Test
    @DisplayName("Criterio 4: un pago a crédito exige cliente, fija el vencimiento y publica el evento")
    void pagoACredito() {
        assertThatThrownBy(() -> pagar(MetodoDePago.CREDITO, "238", null, null, null, 30))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("cliente");

        UUID cliente = UUID.randomUUID();
        venta = ventaConfirmada(cliente);
        pagar(MetodoDePago.CREDITO, "238", null, null, null, 30);

        assertThat(comoElServicio(negocio, "select forma_pago from ventas where id = '" + venta
                + "'")).containsExactly("CREDITO");
        assertThat(comoElServicio(negocio, "select fecha_vencimiento from ventas where id = '"
                + venta + "'")).containsExactly(LocalDate.now().plusDays(30).toString());
        assertThat(comoElServicio(negocio, "select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'venta_a_credito'")).containsExactly("venta_a_credito");
    }

    @Test
    @DisplayName("Criterio 5: un pago con tarjeta guarda la referencia del voucher y la franquicia")
    void pagoConTarjeta() {
        pagar(MetodoDePago.TARJETA_CREDITO, "238", null, "aut-991234", "VISA", null);

        assertThat(comoElServicio(negocio, "select referencia from pagos_venta"))
                .containsExactly("aut-991234");
        assertThat(comoElServicio(negocio, "select franquicia from pagos_venta"))
                .containsExactly("VISA");
    }

    @Test
    @DisplayName("No se cobra una venta que sigue en borrador")
    void noSeCobraEnBorrador() {
        UUID borrador = enContexto(negocio, usuario, SETUP,
                () -> ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                        .id());
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> pagos.registrarPago(
                borrador, new SolicitudDePago(MetodoDePago.EFECTIVO, new BigDecimal("10"),
                        new BigDecimal("10"), null, null, null))))
                .isInstanceOf(com.regenta.comun.errores.ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Los pagos de otro negocio no se ven")
    void aisladoPorNegocio() {
        pagar(MetodoDePago.EFECTIVO, "238", "238", null, null, null);
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from pagos_venta"))
                .containsExactly("0");
    }
}
