package com.regenta.clientes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.clientes.BaseDeClientes;
import com.regenta.clientes.aplicacion.GestionDeCartera.EventoDeCredito;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-022. Cupo de crédito y cartera del cliente. */
class GestionDeCarteraTest extends BaseDeClientes {

    private static final Set<String> DE_ADMIN = Set.of("CLIENTES_CLIENTE_VER",
        "CLIENTES_CLIENTE_CREAR", "CLIENTES_CLIENTE_EDITAR", "CLIENTES_CARTERA_VER");

    @Autowired
    private GestionDeCartera cartera;

    @Autowired
    private GestionDeClientes clientes;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID clienteEn(UUID negocio, String nombre) {
        return enContexto(negocio, admin, DE_ADMIN, () -> clientes.crear(new SolicitudDeCliente(
                "NATURAL", "CC", "" + nombre.hashCode(), null, nombre, "X", null, null, null, null,
                null, null))).id();
    }

    private void habilitarCredito(UUID negocio, UUID cliente, String cupo) {
        enContexto(negocio, admin, DE_ADMIN, () -> cartera.configurarCredito(cliente,
                new SolicitudDeCredito(true, new BigDecimal(cupo), 30)));
    }

    private UUID abrirCuenta(UUID negocio, UUID cliente, String monto, LocalDate vence) {
        final UUID ventaId = UUID.randomUUID();
        enContexto(negocio, admin, DE_ADMIN, () -> cartera.abrirCuentaPorVentaACredito(
                new EventoDeCredito(negocio, cliente, ventaId, "FV-" + ventaId.hashCode(),
                        new BigDecimal(monto), vence)));
        return UUID.fromString(comoElServicio(negocio,
                "select id from cuentas_por_cobrar where origen_id = '" + ventaId + "'").get(0));
    }

    @Test
    @DisplayName("Criterio 1: una venta a crédito que pasa el cupo se avisa")
    void ventaACreditoQueExcedeElCupo() {
        UUID cliente = clienteEn(negocioA, "Ana");
        habilitarCredito(negocioA, cliente, "5000000");
        abrirCuenta(negocioA, cliente, "4800000", LocalDate.now().plusDays(30));

        ResultadoDeCupo excede = enContexto(negocioA, admin, DE_ADMIN,
                () -> cartera.validarCupo(cliente, new BigDecimal("500000")));
        assertThat(excede.cabe()).isFalse();
        assertThat(excede.aviso()).containsIgnoringCase("cupo");
        assertThat(excede.disponible()).isEqualByComparingTo("200000");

        ResultadoDeCupo cabe = enContexto(negocioA, admin, DE_ADMIN,
                () -> cartera.validarCupo(cliente, new BigDecimal("200000")));
        assertThat(cabe.cabe()).isTrue();
    }

    @Test
    @DisplayName("Sin crédito habilitado, ninguna venta a crédito cabe")
    void sinCreditoHabilitadoNoCabe() {
        UUID cliente = clienteEn(negocioA, "Ben");
        ResultadoDeCupo r = enContexto(negocioA, admin, DE_ADMIN,
                () -> cartera.validarCupo(cliente, new BigDecimal("10000")));
        assertThat(r.cabe()).isFalse();
        assertThat(r.aviso()).containsIgnoringCase("habilitado");
    }

    @Test
    @DisplayName("Criterio 2: una cuenta vencida aparece marcada y con los días de mora")
    void cuentaVencidaConDiasDeMora() {
        UUID cliente = clienteEn(negocioA, "Cid");
        habilitarCredito(negocioA, cliente, "3000000");
        abrirCuenta(negocioA, cliente, "900000", LocalDate.now().minusDays(12));

        CarteraDelCliente vista = enContexto(negocioA, admin, DE_ADMIN,
                () -> cartera.verCartera(cliente));

        assertThat(vista.cuentas()).hasSize(1);
        assertThat(vista.cuentas().get(0).vencida()).isTrue();
        assertThat(vista.cuentas().get(0).diasMora()).isEqualTo(12);
        assertThat(vista.saldo()).isEqualByComparingTo("900000");
    }

    @Test
    @DisplayName("Criterio 3: un recaudo parcial baja el saldo y deja la cuenta PARCIAL")
    void recaudoParcial() {
        UUID cliente = clienteEn(negocioA, "Dan");
        habilitarCredito(negocioA, cliente, "3000000");
        UUID cuenta = abrirCuenta(negocioA, cliente, "300000", LocalDate.now().plusDays(15));

        CuentaEnCartera despues = enContexto(negocioA, admin, DE_ADMIN, () -> cartera
                .registrarRecaudo(cuenta, new SolicitudDeRecaudo(new BigDecimal("100000"),
                        "EFECTIVO", "recibo 1")));

        assertThat(despues.estado()).isEqualTo("PARCIAL");
        assertThat(despues.saldo()).isEqualByComparingTo("200000");
        assertThat(enContexto(negocioA, admin, DE_ADMIN, () -> cartera.verCartera(cliente)).saldo())
                .isEqualByComparingTo("200000");
    }

    @Test
    @DisplayName("Criterio 4: un recaudo que iguala el monto deja la cuenta PAGADA y ajusta el saldo")
    void recaudoTotal() {
        UUID cliente = clienteEn(negocioA, "Eva");
        habilitarCredito(negocioA, cliente, "3000000");
        UUID cuenta = abrirCuenta(negocioA, cliente, "300000", LocalDate.now().plusDays(15));

        CuentaEnCartera despues = enContexto(negocioA, admin, DE_ADMIN, () -> cartera
                .registrarRecaudo(cuenta, new SolicitudDeRecaudo(new BigDecimal("300000"),
                        "TRANSFERENCIA", null)));

        assertThat(despues.estado()).isEqualTo("PAGADA");
        assertThat(despues.saldo()).isEqualByComparingTo("0");
        assertThat(enContexto(negocioA, admin, DE_ADMIN, () -> cartera.verCartera(cliente)).saldo())
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Un recaudo mayor que el saldo se rechaza")
    void recaudoQueExcedeElSaldo() {
        UUID cliente = clienteEn(negocioA, "Fio");
        habilitarCredito(negocioA, cliente, "3000000");
        UUID cuenta = abrirCuenta(negocioA, cliente, "300000", LocalDate.now().plusDays(15));

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN, () -> cartera
                .registrarRecaudo(cuenta, new SolicitudDeRecaudo(new BigDecimal("400000"),
                        "EFECTIVO", null))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("La base impide dejar el saldo negativo o mayor que el monto (ck_saldo)")
    void ckSaldoEnLaBase() {
        UUID cliente = clienteEn(negocioA, "Gil");
        habilitarCredito(negocioA, cliente, "3000000");
        UUID cuenta = abrirCuenta(negocioA, cliente, "300000", LocalDate.now().plusDays(15));

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA,
                "update cuentas_por_cobrar set saldo = -1 where id = '" + cuenta + "'"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA,
                "update cuentas_por_cobrar set saldo = 999999 where id = '" + cuenta + "'"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("La cartera de un negocio no se ve desde otro")
    void aisladoPorNegocio() {
        UUID clienteA = clienteEn(negocioA, "Hia");
        habilitarCredito(negocioA, clienteA, "3000000");
        abrirCuenta(negocioA, clienteA, "300000", LocalDate.now().plusDays(15));

        assertThat(comoElServicio(negocioB, "select count(*) from cuentas_por_cobrar"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Configurar el crédito exige CLIENTES_CLIENTE_EDITAR; ver la cartera, CLIENTES_CARTERA_VER")
    void permisos() {
        UUID cliente = clienteEn(negocioA, "Ivo");

        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("CLIENTES_CARTERA_VER"),
                () -> cartera.configurarCredito(cliente, new SolicitudDeCredito(true,
                        new BigDecimal("1000000"), 30))))
                .isInstanceOf(SinPermisoException.class);

        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("CLIENTES_CLIENTE_EDITAR"),
                () -> cartera.verCartera(cliente)))
                .isInstanceOf(SinPermisoException.class);
    }
}
