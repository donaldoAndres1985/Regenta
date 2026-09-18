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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.ventas.BaseDeVentas;

/**
 * HU-040. Vender a plazo al cliente con cupo aprobado. Lo que el cupo, el
 * saldo y la mora valen lo sabe servicio-clientes (HU-022): aquí se prueba qué
 * hace Ventas con esa respuesta, con un doble en lugar de la llamada real.
 */
@Import(VentaACreditoTest.Dobles.class)
class VentaACreditoTest extends BaseDeVentas {

    private static final Set<String> VENDEDOR =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");
    private static final Set<String> GERENTE = Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER",
            "VENTAS_VENTA_CONFIRMAR", "VENTAS_CREDITO_APROBAR");

    @TestConfiguration
    static class Dobles {
        @Bean
        @Primary
        ConsultaDeCreditoDoble consultaDeCreditoDoble() {
            return new ConsultaDeCreditoDoble();
        }
    }

    /** Devuelve lo que el test le ponga: es servicio-clientes, sin la red. */
    static class ConsultaDeCreditoDoble implements ConsultaDeCredito {
        CreditoDelCliente respuesta = CreditoDelCliente.habilitado(
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("1000000"), 30, false, 0);

        @Override
        public CreditoDelCliente consultar(UUID clienteId) {
            return respuesta;
        }
    }

    @Autowired
    private GestionDeVentas ventas;
    @Autowired
    private GestionDeVentaACredito credito;
    @Autowired
    private SagaDeConfirmacionDeVenta saga;
    @Autowired
    private ConsultaDeCreditoDoble clientes;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private final UUID cliente = UUID.randomUUID();

    private UUID venta;

    @BeforeEach
    void preparar() {
        clientes.respuesta = CreditoDelCliente.habilitado(
                new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("1000000"), 30, false, 0);
        venta = enContexto(negocio, usuario, VENDEDOR, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, cliente, null, "MOSTRADOR"))
                    .id();
            // 2 x 32000 + IVA 19% = 76160
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Bandeja", "UND",
                    new BigDecimal("2"), new BigDecimal("32000"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("12000")));
            return id;
        });
    }

    private String columna(String columna) {
        return comoElServicio(negocio,
                "select " + columna + " from ventas where id = '" + venta + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: un cliente sin crédito habilitado no puede comprar a plazo")
    void sinCreditoHabilitadoSeRechaza() {
        clientes.respuesta = CreditoDelCliente.sinCredito();

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(false))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("credito");

        assertThat(columna("estado")).isEqualTo("BORRADOR");
        assertThat(columna("forma_pago")).isEqualTo("CONTADO");
    }

    @Test
    @DisplayName("Criterio 2: si el saldo más esta venta pasan el cupo, se rechaza diciendo cuánto sobra")
    void elCupoInsuficienteSeRechazaConElExceso() {
        clientes.respuesta = CreditoDelCliente.habilitado(new BigDecimal("100000"),
                new BigDecimal("60000"), new BigDecimal("40000"), 30, false, 0);

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(false))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("36160")   // 76160 de la venta - 40000 disponibles
                .hasMessageContaining("40000");

        assertThat(columna("estado")).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("La venta a crédito queda con su forma de pago, su saldo y su fecha de vencimiento")
    void laVentaACreditoGuardaSusCondiciones() {
        enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(false)));

        assertThat(columna("forma_pago")).isEqualTo("CREDITO");
        assertThat(columna("saldo_pendiente")).isEqualTo("76160.0000");
        assertThat(columna("fecha_vencimiento"))
                .isEqualTo(LocalDate.now().plusDays(30).toString());
        assertThat(columna("estado"))
                .as("se confirma como cualquier otra: pasa por la saga de stock")
                .isEqualTo("PENDIENTE_STOCK");
    }

    @Test
    @DisplayName("Criterio 3: al confirmarse de verdad, se publica venta_a_credito para que Clientes abra la cuenta")
    void alConfirmarsePublicaVentaACredito() {
        enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(false)));
        UUID correlacion = UUID.fromString(comoElServicio(negocio,
                "select correlacion_id from sagas where agregado_id = '" + venta + "'").get(0));

        enContexto(negocio, usuario, VENDEDOR, () -> saga.alStockReservado(correlacion));

        assertThat(columna("estado")).isEqualTo("CONFIRMADA");
        String payload = comoElServicio(negocio, "select payload from outbox_eventos "
                + "where negocio_id = '" + negocio + "' and tipo_evento = 'venta_a_credito'").get(0);
        assertThat(payload)
                .contains(cliente.toString())
                .contains(venta.toString())
                .contains("76160")
                .contains(LocalDate.now().plusDays(30).toString());
    }

    @Test
    @DisplayName("Una venta de contado no publica venta_a_credito al confirmarse")
    void laDeContadoNoPublicaCredito() {
        enContexto(negocio, usuario, VENDEDOR, () -> ventas.confirmar(venta));
        UUID correlacion = UUID.fromString(comoElServicio(negocio,
                "select correlacion_id from sagas where agregado_id = '" + venta + "'").get(0));

        enContexto(negocio, usuario, VENDEDOR, () -> saga.alStockReservado(correlacion));

        assertThat(comoElServicio(negocio, "select count(*) from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'venta_a_credito'")).containsExactly("0");
    }

    @Test
    @DisplayName("Criterio 4: con cartera vencida se advierte y no pasa sin autorización")
    void carteraVencidaAvisaYExigeAutorizacion() {
        clientes.respuesta = CreditoDelCliente.habilitado(new BigDecimal("1000000"),
                new BigDecimal("200000"), new BigDecimal("800000"), 30, true, 45);

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(false))))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("45")
                .hasMessageContaining("autorizacion");

        assertThat(columna("estado")).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("Criterio 4: autorizar la venta con cartera vencida exige el permiso, no basta con pedirlo")
    void autorizarSinPermisoNoAlcanza() {
        clientes.respuesta = CreditoDelCliente.habilitado(new BigDecimal("1000000"),
                new BigDecimal("200000"), new BigDecimal("800000"), 30, true, 45);

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(true))))
                .isInstanceOf(SinPermisoException.class);

        assertThat(columna("estado")).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("Criterio 4: con el permiso, un rol superior la autoriza y queda constancia de quién")
    void elRolSuperiorAutorizaYQuedaConstancia() {
        clientes.respuesta = CreditoDelCliente.habilitado(new BigDecimal("1000000"),
                new BigDecimal("200000"), new BigDecimal("800000"), 30, true, 45);
        UUID gerente = UUID.randomUUID();

        enContexto(negocio, gerente, GERENTE,
                () -> credito.confirmarACredito(venta, new SolicitudDeVentaACredito(true)));

        assertThat(columna("forma_pago")).isEqualTo("CREDITO");
        assertThat(columna("estado")).isEqualTo("PENDIENTE_STOCK");
        assertThat(columna("nota"))
                .as("quién autorizó vender a un cliente con mora tiene que quedar escrito")
                .contains(gerente.toString());
    }

    @Test
    @DisplayName("Sin cliente no hay venta a crédito: a quién se le cobraría después")
    void sinClienteNoHayCredito() {
        UUID sinCliente = enContexto(negocio, usuario, VENDEDOR, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR")).id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-2", "Prod", "UND",
                    BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("40")));
            return id;
        });

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> credito.confirmarACredito(sinCliente, new SolicitudDeVentaACredito(false))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("cliente");
    }
}
