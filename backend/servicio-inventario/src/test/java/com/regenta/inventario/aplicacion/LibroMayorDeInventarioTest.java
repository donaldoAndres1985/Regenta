package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/** HU-030. Libro mayor de inventario: append-only y cuadre con la proyeccion. */
class LibroMayorDeInventarioTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR");

    @Autowired
    private LibroMayorDeInventario libro;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID producto;
    private UUID bodega;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-1", null, "Prod", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    false, false, false, false, null)).id();
            bodega = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            return null;
        });
    }

    private RegistroDeMovimiento registrar(TipoMovimiento tipo, String cantidad, String idem) {
        return enContexto(negocio, usuario, Set.of(),
                () -> libro.registrar(new SolicitudDeMovimiento(producto, bodega, tipo,
                        new BigDecimal(cantidad), OrigenMovimiento.CARGA_INICIAL, null, "prueba",
                        idem)));
    }

    private String cantidadExistencia() {
        return comoElServicio(negocio, "select cantidad from existencias where producto_id = '"
                + producto + "' and bodega_id = '" + bodega + "'").stream().findFirst().orElse("0");
    }

    @Test
    @DisplayName("Criterio 1: todo cambio de stock inserta un movimiento con sus datos")
    void insertaElMovimiento() {
        registrar(TipoMovimiento.ENTRADA_COMPRA, "100", "idem-1");

        assertThat(comoElServicio(negocio, "select tipo from movimientos_inventario"))
                .containsExactly("ENTRADA_COMPRA");
        assertThat(comoElServicio(negocio, "select signo from movimientos_inventario"))
                .containsExactly("1");
        assertThat(comoElServicio(negocio, "select saldo_posterior from movimientos_inventario"))
                .containsExactly("100.000000");
        assertThat(comoElServicio(negocio, "select usuario_id from movimientos_inventario"))
                .containsExactly(usuario.toString());
        assertThat(cantidadExistencia()).isEqualTo("100.000000");
    }

    @Test
    @DisplayName("Criterio 2: un movimiento no se actualiza ni se borra (append-only)")
    void appendOnly() {
        registrar(TipoMovimiento.ENTRADA_COMPRA, "50", "idem-1");

        assertThatThrownBy(() -> ejecutarComoElServicio(negocio,
                "UPDATE movimientos_inventario SET motivo = 'editado'"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rechazo");
        assertThatThrownBy(() -> ejecutarComoElServicio(negocio,
                "DELETE FROM movimientos_inventario"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 3: el mismo evento entregado dos veces no descuenta dos veces")
    void idempotente() {
        registrar(TipoMovimiento.ENTRADA_COMPRA, "100", "idem-dup");
        RegistroDeMovimiento segundo = registrar(TipoMovimiento.ENTRADA_COMPRA, "100", "idem-dup");

        assertThat(segundo.duplicado()).isTrue();
        assertThat(comoElServicio(negocio, "select count(*) from movimientos_inventario"))
                .containsExactly("1");
        assertThat(cantidadExistencia()).isEqualTo("100.000000");
    }

    @Test
    @DisplayName("Criterio 4: sumar el libro da exactamente existencias.cantidad")
    void elLibroCuadraConLaProyeccion() {
        registrar(TipoMovimiento.ENTRADA_COMPRA, "100", "m1");
        registrar(TipoMovimiento.SALIDA_VENTA, "30", "m2");
        registrar(TipoMovimiento.ENTRADA_COMPRA, "10", "m3");

        BigDecimal segunElLibro = enContexto(negocio, usuario, Set.of(),
                () -> libro.saldoSegunElLibro(producto, bodega));

        assertThat(segunElLibro).isEqualByComparingTo("80");
        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("80");
    }

    @Test
    @DisplayName("Criterio 5: un error se corrige con un movimiento contrario, no editando")
    void correccionConMovimientoContrario() {
        registrar(TipoMovimiento.ENTRADA_COMPRA, "100", "malo");
        registrar(TipoMovimiento.SALIDA_AJUSTE, "100", "correccion");

        assertThat(comoElServicio(negocio, "select count(*) from movimientos_inventario"))
                .containsExactly("2");
        assertThat(comoElServicio(negocio, "select saldo_posterior from movimientos_inventario"
                + " order by ocurrido_en")).containsExactly("100.000000", "0.000000");
        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El libro de otro negocio no se mezcla")
    void aisladoPorNegocio() {
        registrar(TipoMovimiento.ENTRADA_COMPRA, "5", "n1");

        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from movimientos_inventario"))
                .containsExactly("0");
    }
}
