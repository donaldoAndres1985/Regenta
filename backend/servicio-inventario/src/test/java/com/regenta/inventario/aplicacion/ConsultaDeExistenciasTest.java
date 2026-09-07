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

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.inventario.BaseDeInventario;

/** HU-029. El stock de un producto por bodega y el total. */
class ConsultaDeExistenciasTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_PRODUCTO_VER", "INVENTARIO_BODEGA_CREAR",
            "INVENTARIO_EXISTENCIA_VER");

    @Autowired
    private ConsultaDeExistencias consulta;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID producto;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-1", null, "Prod", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    false, false, false, false, null)).id();
            return null;
        });
    }

    private UUID bodega(String codigo) {
        return enContexto(negocio, usuario, SETUP,
                () -> bodegas.crear(new SolicitudDeBodega(codigo, codigo, null, null))).id();
    }

    private void existencia(UUID bodega, String cantidad, String reservada) {
        comoSuperusuario("INSERT INTO inventario.existencias (negocio_id, producto_id, bodega_id,"
                + " cantidad, cantidad_reservada) VALUES ('" + negocio + "','" + producto + "','"
                + bodega + "'," + cantidad + "," + reservada + ")");
    }

    @Test
    @DisplayName("Criterio 1: el stock se ve por bodega y con el total")
    void stockPorBodegaYTotal() {
        existencia(bodega("B1"), "5", "0");
        existencia(bodega("B2"), "3", "0");
        existencia(bodega("B3"), "2", "0");

        StockDeProducto stock = enContexto(negocio, usuario, SETUP,
                () -> consulta.delProducto(producto));

        assertThat(stock.porBodega()).hasSize(3);
        assertThat(stock.total()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Criterio 2: cantidad_disponible = cantidad - reservada, la calcula la base")
    void disponibleLoCalculaLaBase() {
        UUID b1 = bodega("B1");
        existencia(b1, "10", "3");

        StockDeProducto stock = enContexto(negocio, usuario, SETUP,
                () -> consulta.delProducto(producto));

        assertThat(stock.porBodega().get(0).cantidadDisponible()).isEqualByComparingTo("7");
        assertThat(stock.totalDisponible()).isEqualByComparingTo("7");
        assertThat(comoElServicio(negocio,
                "select cantidad_disponible from existencias where bodega_id = '" + b1 + "'"))
                .containsExactly("7.000000");
    }

    @Test
    @DisplayName("Un producto sin existencias tiene stock cero")
    void sinExistencias() {
        StockDeProducto stock = enContexto(negocio, usuario, SETUP,
                () -> consulta.delProducto(producto));

        assertThat(stock.porBodega()).isEmpty();
        assertThat(stock.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El stock de un producto de otro negocio no se ve")
    void productoDeOtroNegocio() {
        assertThatThrownBy(() -> enContexto(UUID.randomUUID(), usuario, SETUP,
                () -> consulta.delProducto(producto)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
