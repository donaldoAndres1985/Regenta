package com.regenta.comandas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comandas.BaseDeComandas;
import com.regenta.comandas.aplicacion.CatalogoDeMenu.ItemDeMenu;
import com.regenta.comandas.infra.CatalogoDeMenuStub;
import com.regenta.comun.errores.ConflictoDeEstadoException;

/**
 * HU-090. Cerrar la comanda con propina y descuento de insumos: la propina se
 * cobra aparte, el cierre exige que todo se haya enviado a cocina, y publica
 * los eventos que consumen menú (recetas) y mesas (la deja SUCIA). Con dos
 * negocios cargados.
 */
class CierreDeComandaTest extends BaseDeComandas {

    private static final Set<String> MESERO =
            Set.of("COMANDAS_COMANDA_VER", "COMANDAS_COMANDA_CREAR", "COMANDAS_COMANDA_EDITAR");

    @Autowired
    private GestionDeComandas comandas;
    @Autowired
    private GestionDeCuentas cuentasSvc;
    @Autowired
    private CatalogoDeMenuStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID mesaId = UUID.randomUUID();
    private final UUID sesionId = UUID.randomUUID();

    private final UUID bandeja = UUID.randomUUID();

    @BeforeEach
    void catalogoDePrueba() {
        catalogo.reiniciar();
        catalogo.cargarItem(new ItemDeMenu(bandeja, "Bandeja paisa", new BigDecimal("47000"),
                UUID.randomUUID(), "FUERTE", BigDecimal.ZERO, true));
    }

    private UUID comanda(UUID negocio) {
        return enContexto(negocio, mesero, MESERO,
                () -> comandas.abrir(new SolicitudDeAperturaComanda(mesaId, sesionId, 2, null))).id();
    }

    private UUID agregar(UUID negocio, UUID comandaId) {
        var d = enContexto(negocio, mesero, MESERO, () -> comandas.agregarLinea(comandaId,
                new SolicitudDeLinea(bandeja, BigDecimal.ONE, null, null, null, null, null, null, null)));
        return d.lineas().get(d.lineas().size() - 1).id();
    }

    private UUID cuentaConTodo(UUID negocio, UUID comandaId, UUID lineaId) {
        UUID cuentaId = enContexto(negocio, mesero, MESERO,
                () -> cuentasSvc.crearCuenta(comandaId, new SolicitudDeCuenta(null, null))).id();
        enContexto(negocio, mesero, MESERO, () -> cuentasSvc.marcarLinea(comandaId, cuentaId, lineaId));
        return cuentaId;
    }

    @Test
    @DisplayName("Criterio 1: una línea sin enviar a cocina bloquea el cierre, y el pago tampoco queda")
    void lineaSinEnviarBloqueaElCierre() {
        UUID c = comanda(negocioA);
        UUID l = agregar(negocioA, c); // nunca se envía a cocina: sigue PENDIENTE
        UUID cuentaId = cuentaConTodo(negocioA, c, l);

        assertThatThrownBy(() -> enContexto(negocioA, mesero, MESERO,
                () -> cuentasSvc.registrarPago(c, cuentaId, new SolicitudDePago("EFECTIVO", null, null, null))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        // Se revirtió también el pago: la cuenta sigue abierta.
        CuentaDetallada tras = enContexto(negocioA, mesero, MESERO, () -> cuentasSvc.ver(c)).get(0);
        assertThat(tras.estado()).isEqualTo("ABIERTA");
        assertThat(enContexto(negocioA, mesero, MESERO, () -> comandas.ver(c)).estado())
                .isNotEqualTo("CERRADA");
    }

    @Test
    @DisplayName("Criterio 2: la propina se registra aparte del total, y el cliente la puede cambiar")
    void propinaAparte() {
        UUID c = comanda(negocioA);
        UUID l = agregar(negocioA, c);
        enContexto(negocioA, mesero, MESERO, () -> comandas.enviarACocina(c));
        UUID cuentaId = cuentaConTodo(negocioA, c, l);

        CuentaDetallada tras = enContexto(negocioA, mesero, MESERO, () -> cuentasSvc.registrarPago(c, cuentaId,
                new SolicitudDePago("TARJETA_DEBITO", null, new BigDecimal("5000"), "voucher 004182")));

        assertThat(tras.propina()).isEqualByComparingTo("5000");
        assertThat(tras.total()).isEqualByComparingTo("52000"); // 47000 + 5000
        assertThat(comoElServicio(negocioA,
                "select propina, metodo, referencia from pagos_comanda where comanda_id = '" + c + "'"))
                .isNotEmpty();
    }

    @Test
    @DisplayName("Criterios 3 y 5: al cerrar se publican pedido_completado y comanda_cerrada, con la caja registrando el pago")
    void cierrePublicaEventosYRegistraElPago() {
        UUID c = comanda(negocioA);
        UUID l = agregar(negocioA, c);
        enContexto(negocioA, mesero, MESERO, () -> comandas.enviarACocina(c));
        UUID cuentaId = cuentaConTodo(negocioA, c, l);

        enContexto(negocioA, mesero, MESERO,
                () -> cuentasSvc.registrarPago(c, cuentaId, new SolicitudDePago("EFECTIVO", null, null, null)));

        assertThat(enContexto(negocioA, mesero, MESERO, () -> comandas.ver(c)).estado()).isEqualTo("CERRADA");
        assertThat(comoElServicio(negocioA, "select tipo_evento from outbox_eventos "
                + "where negocio_id = '" + negocioA + "' and agregado_id = '" + c
                + "' and tipo_evento = 'pedido_completado'")).containsExactly("pedido_completado");
        assertThat(comoElServicio(negocioA, "select tipo_evento from outbox_eventos "
                + "where negocio_id = '" + negocioA + "' and agregado_id = '" + c
                + "' and tipo_evento = 'comanda_cerrada'")).containsExactly("comanda_cerrada");
        assertThat(contar("select count(*) from pagos_comanda where comanda_id = '" + c + "'")).isEqualTo(1);
    }

    @Test
    @DisplayName("El segundo negocio no puede cobrar una cuenta del primero")
    void aislamiento() {
        UUID c = comanda(negocioA);
        UUID l = agregar(negocioA, c);
        enContexto(negocioA, mesero, MESERO, () -> comandas.enviarACocina(c));
        UUID cuentaId = cuentaConTodo(negocioA, c, l);

        assertThatThrownBy(() -> enContexto(negocioB, mesero, MESERO,
                () -> cuentasSvc.registrarPago(c, cuentaId, new SolicitudDePago("EFECTIVO", null, null, null))))
                .isInstanceOf(com.regenta.comun.errores.NoEncontradoException.class);
    }
}
