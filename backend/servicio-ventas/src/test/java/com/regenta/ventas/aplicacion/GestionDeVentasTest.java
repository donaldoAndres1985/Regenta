package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.ventas.BaseDeVentas;

/** HU-037. Crear una venta en borrador con sus líneas. */
class GestionDeVentasTest extends BaseDeVentas {

    private static final Set<String> SETUP =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");

    @Autowired
    private GestionDeVentas ventas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID otroNegocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID venta;

    @BeforeEach
    void preparar() {
        venta = enContexto(negocio, usuario, SETUP,
                () -> ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                        .id());
    }

    private LineaDeVenta agregar(String sku, String cantidad, String precio, String impuestoPct) {
        return enContexto(negocio, usuario, SETUP, () -> ventas.agregarLinea(venta,
                new SolicitudDeLinea(UUID.randomUUID(), sku, "Producto " + sku, "UND",
                        new BigDecimal(cantidad), new BigDecimal(precio), BigDecimal.ZERO, "IVA19",
                        new BigDecimal(impuestoPct), new BigDecimal("40"))));
    }

    private String columnaVenta(String columna) {
        return comoElServicio(negocio,
                "select " + columna + " from ventas where id = '" + venta + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: la línea guarda copia del SKU, nombre, precio, impuesto y costo")
    void laLineaGuardaLosSnapshots() {
        agregar("SKU-1", "2", "100", "19");

        assertThat(comoElServicio(negocio, "select sku_snapshot from venta_lineas"))
                .containsExactly("SKU-1");
        assertThat(comoElServicio(negocio, "select nombre_snapshot from venta_lineas"))
                .containsExactly("Producto SKU-1");
        assertThat(comoElServicio(negocio, "select precio_unitario from venta_lineas"))
                .containsExactly("100.0000");
        assertThat(comoElServicio(negocio, "select impuesto_pct from venta_lineas"))
                .containsExactly("19.0000");
        assertThat(comoElServicio(negocio, "select costo_unitario_snapshot from venta_lineas"))
                .containsExactly("40.0000");
    }

    @Test
    @DisplayName("Criterio 2: cambiar la cantidad no toca el precio de la línea (es una copia)")
    void elPrecioDeLaLineaNoCambia() {
        LineaDeVenta linea = agregar("SKU-2", "1", "250", "0");
        enContexto(negocio, usuario, SETUP, () -> ventas.cambiarCantidad(venta, linea.linea(),
                new BigDecimal("9")));

        assertThat(comoElServicio(negocio, "select precio_unitario from venta_lineas"))
                .containsExactly("250.0000");
        assertThat(comoElServicio(negocio, "select cantidad from venta_lineas"))
                .containsExactly("9.000000");
    }

    @Test
    @DisplayName("Criterio 3: cambiar la cantidad recalcula y persiste los totales de la venta")
    void losTotalesSeRecalculanYSePersisten() {
        LineaDeVenta linea = agregar("SKU-3", "2", "100", "19");

        assertThat(new BigDecimal(columnaVenta("subtotal"))).isEqualByComparingTo("200");
        assertThat(new BigDecimal(columnaVenta("impuesto_total"))).isEqualByComparingTo("38");
        assertThat(new BigDecimal(columnaVenta("total"))).isEqualByComparingTo("238");

        enContexto(negocio, usuario, SETUP, () -> ventas.cambiarCantidad(venta, linea.linea(),
                new BigDecimal("5")));

        assertThat(new BigDecimal(columnaVenta("subtotal"))).isEqualByComparingTo("500");
        assertThat(new BigDecimal(columnaVenta("impuesto_total"))).isEqualByComparingTo("95");
        assertThat(new BigDecimal(columnaVenta("total"))).isEqualByComparingTo("595");
    }

    @Test
    @DisplayName("Criterio 4: el número de venta es consecutivo por negocio y no choca con otro negocio")
    void elNumeroEsConsecutivoPorNegocio() {
        String a2 = enContexto(negocio, usuario, SETUP,
                () -> ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                        .numero());
        String b1 = enContexto(otroNegocio, usuario, SETUP,
                () -> ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                        .numero());
        String b2 = enContexto(otroNegocio, usuario, SETUP,
                () -> ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                        .numero());

        // La venta del @BeforeEach fue la 1 del negocio A.
        assertThat(columnaVenta("numero")).isEqualTo("1");
        assertThat(a2).isEqualTo("2");
        assertThat(b1).isEqualTo("1");
        assertThat(b2).isEqualTo("2");
    }

    @Test
    @DisplayName("Criterio 5: una venta que salió de borrador ya no admite editar sus líneas")
    void laVentaConfirmadaNoSeEdita() {
        agregar("SKU-5", "1", "100", "19");
        enContexto(negocio, usuario, SETUP, () -> ventas.confirmar(venta));

        // Confirmar arranca la saga (HU-038): la venta pasa a PENDIENTE_STOCK y
        // ya no es BORRADOR, así que no se edita.
        assertThat(columnaVenta("estado")).isEqualTo("PENDIENTE_STOCK");
        assertThatThrownBy(() -> agregar("SKU-6", "1", "50", "19"))
                .isInstanceOf(ConflictoDeEstadoException.class);
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP,
                () -> ventas.cambiarCantidad(venta, (short) 1, new BigDecimal("3"))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("La venta de otro negocio no se ve")
    void aisladoPorNegocio() {
        assertThat(comoElServicio(otroNegocio, "select count(*) from ventas"))
                .containsExactly("0");
    }
}
