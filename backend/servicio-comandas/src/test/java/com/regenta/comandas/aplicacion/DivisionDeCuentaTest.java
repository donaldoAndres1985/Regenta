package com.regenta.comandas.aplicacion;

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

import com.regenta.comandas.BaseDeComandas;
import com.regenta.comandas.aplicacion.CatalogoDeMenu.ItemDeMenu;
import com.regenta.comandas.infra.CatalogoDeMenuStub;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;

/**
 * HU-089. Dividir la cuenta entre comensales: por ítem, repartiendo una línea
 * compartida, o en partes iguales. Con dos negocios cargados.
 */
class DivisionDeCuentaTest extends BaseDeComandas {

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

    private final UUID bandeja = UUID.randomUUID();
    private final UUID limonada = UUID.randomUUID();

    @BeforeEach
    void catalogoDePrueba() {
        catalogo.reiniciar();
        catalogo.cargarItem(new ItemDeMenu(bandeja, "Bandeja paisa", new BigDecimal("40000"),
                UUID.randomUUID(), "FUERTE", BigDecimal.ZERO, true));
        catalogo.cargarItem(new ItemDeMenu(limonada, "Limonada de coco", new BigDecimal("20000"),
                UUID.randomUUID(), "BEBIDA", BigDecimal.ZERO, true));
    }

    private UUID comanda(UUID negocio) {
        return enContexto(negocio, mesero, MESERO, () -> comandas.abrir(
                new SolicitudDeAperturaComanda(UUID.randomUUID(), UUID.randomUUID(), 2, null))).id();
    }

    private UUID agregar(UUID negocio, UUID comandaId, UUID item, String cantidad) {
        var d = enContexto(negocio, mesero, MESERO, () -> comandas.agregarLinea(comandaId,
                new SolicitudDeLinea(item, new BigDecimal(cantidad), null, null, null, null, null, null, null)));
        return d.lineas().get(d.lineas().size() - 1).id();
    }

    private CuentaDetallada crearCuenta(UUID negocio, UUID comandaId, String etiqueta) {
        return enContexto(negocio, mesero, MESERO,
                () -> cuentasSvc.crearCuenta(comandaId, new SolicitudDeCuenta(etiqueta, null)));
    }

    private List<CuentaDetallada> marcar(UUID negocio, UUID comandaId, UUID cuentaId, UUID lineaId) {
        return enContexto(negocio, mesero, MESERO, () -> cuentasSvc.marcarLinea(comandaId, cuentaId, lineaId));
    }

    @Test
    @DisplayName("Criterio 1: dividir por ítem asigna cada línea a una cuenta y los totales cuadran")
    void dividirPorItem() {
        UUID c = comanda(negocioA);
        UUID l1 = agregar(negocioA, c, bandeja, "1"); // 40000
        UUID l2 = agregar(negocioA, c, limonada, "1"); // 20000
        UUID cuentaA = crearCuenta(negocioA, c, "Camilo").id();
        UUID cuentaB = crearCuenta(negocioA, c, "resto de la mesa").id();

        marcar(negocioA, c, cuentaA, l1);
        List<CuentaDetallada> tras = marcar(negocioA, c, cuentaB, l2);

        CuentaDetallada primera = tras.stream().filter(x -> x.id().equals(cuentaA)).findFirst().orElseThrow();
        CuentaDetallada segunda = tras.stream().filter(x -> x.id().equals(cuentaB)).findFirst().orElseThrow();
        assertThat(primera.total()).isEqualByComparingTo("40000");
        assertThat(segunda.total()).isEqualByComparingTo("20000");

        ComandaDetallada original = enContexto(negocioA, mesero, MESERO, () -> comandas.ver(c));
        assertThat(primera.total().add(segunda.total())).isEqualByComparingTo(original.total());
    }

    @Test
    @DisplayName("Criterio 2: una bebida compartida entre dos cuentas reparte al 50% y suma el 100%")
    void repartirLineaCompartida() {
        UUID c = comanda(negocioA);
        UUID bebida = agregar(negocioA, c, limonada, "1"); // 20000
        UUID cuentaA = crearCuenta(negocioA, c, null).id();
        UUID cuentaB = crearCuenta(negocioA, c, null).id();

        marcar(negocioA, c, cuentaA, bebida);
        List<CuentaDetallada> tras = marcar(negocioA, c, cuentaB, bebida);

        CuentaDetallada primera = tras.stream().filter(x -> x.id().equals(cuentaA)).findFirst().orElseThrow();
        CuentaDetallada segunda = tras.stream().filter(x -> x.id().equals(cuentaB)).findFirst().orElseThrow();
        assertThat(primera.lineas().get(0).proporcion()).isEqualByComparingTo("0.5");
        assertThat(segunda.lineas().get(0).proporcion()).isEqualByComparingTo("0.5");
        assertThat(primera.lineas().get(0).proporcion().add(segunda.lineas().get(0).proporcion()))
                .isEqualByComparingTo("1");
        assertThat(primera.total()).isEqualByComparingTo("10000");
        assertThat(segunda.total()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("Criterio 3: dividir en partes iguales entre cuatro deja a cada cuenta con la cuarta parte")
    void partesIguales() {
        UUID c = comanda(negocioA);
        agregar(negocioA, c, bandeja, "1"); // 40000
        agregar(negocioA, c, limonada, "1"); // 20000 -> total 60000

        List<CuentaDetallada> divididas = enContexto(negocioA, mesero, MESERO,
                () -> cuentasSvc.dividirEnPartesIguales(c, 4));

        assertThat(divididas).hasSize(4);
        for (CuentaDetallada cuenta : divididas) {
            assertThat(cuenta.total()).isEqualByComparingTo("15000"); // 60000 / 4
        }
    }

    @Test
    @DisplayName("Criterio 4: una cuenta pagada rechaza que le muevan líneas")
    void cuentaPagadaRechaza() {
        UUID c = comanda(negocioA);
        UUID l1 = agregar(negocioA, c, bandeja, "1");
        UUID l2 = agregar(negocioA, c, limonada, "1");
        UUID cuentaId = crearCuenta(negocioA, c, null).id();
        marcar(negocioA, c, cuentaId, l1);
        enContexto(negocioA, mesero, MESERO, () -> cuentasSvc.marcarPagada(c, cuentaId));

        assertThatThrownBy(() -> marcar(negocioA, c, cuentaId, l2))
                .isInstanceOf(ConflictoDeEstadoException.class);
        assertThatThrownBy(() -> enContexto(negocioA, mesero, MESERO,
                () -> cuentasSvc.desmarcarLinea(c, cuentaId, l1)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 5: al pagarse la última cuenta abierta, la comanda se cierra sola")
    void ultimaCuentaCierraLaComanda() {
        UUID c = comanda(negocioA);
        UUID l1 = agregar(negocioA, c, bandeja, "1");
        UUID l2 = agregar(negocioA, c, limonada, "1");
        UUID cuentaA = crearCuenta(negocioA, c, null).id();
        UUID cuentaB = crearCuenta(negocioA, c, null).id();
        marcar(negocioA, c, cuentaA, l1);
        marcar(negocioA, c, cuentaB, l2);

        enContexto(negocioA, mesero, MESERO, () -> cuentasSvc.marcarPagada(c, cuentaA));
        assertThat(enContexto(negocioA, mesero, MESERO, () -> comandas.ver(c)).estado())
                .isNotEqualTo("CERRADA");

        enContexto(negocioA, mesero, MESERO, () -> cuentasSvc.marcarPagada(c, cuentaB));
        assertThat(enContexto(negocioA, mesero, MESERO, () -> comandas.ver(c)).estado())
                .isEqualTo("CERRADA");
    }

    @Test
    @DisplayName("El segundo negocio no ve ni toca las cuentas del primero")
    void aislamiento() {
        UUID c = comanda(negocioA);
        UUID l1 = agregar(negocioA, c, bandeja, "1");
        UUID cuentaId = crearCuenta(negocioA, c, null).id();

        assertThatThrownBy(() -> enContexto(negocioB, mesero, MESERO, () -> cuentasSvc.ver(c)))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> marcar(negocioB, c, cuentaId, l1)).isInstanceOf(NoEncontradoException.class);
    }
}
