package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.SinPermisoException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/**
 * HU-138 criterio 6, lado proveedor. El costo de un insumo es el de su última
 * entrada con costo (decisión del 27-oct en #258): el libro mayor lo fija en
 * {@code productos.costo_ultimo} y publica {@code costo_producto_actualizado},
 * que es lo que hace recalcular las recetas del menú (HU-079 criterio 2). Con
 * dos negocios cargados.
 */
class CostoUltimoDeEntradaTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_LOTE_REGISTRAR",
            "INVENTARIO_AJUSTE_CREAR", "INVENTARIO_AJUSTE_APLICAR", "INVENTARIO_AJUSTE_VER");
    private static final Set<String> VER = Set.of("INVENTARIO_PRODUCTO_VER");

    @Autowired
    private LibroMayorDeInventario libro;
    @Autowired
    private GestionDeCategorias categorias;
    @Autowired
    private GestionDeProductos productos;
    @Autowired
    private GestionDeBodegas bodegas;
    @Autowired
    private GestionDeLotes lotes;
    @Autowired
    private GestionDeAjustes ajustes;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID carne;
    private UUID arroz;
    private UUID leche;
    private UUID bodega;

    @BeforeEach
    void preparar() {
        enContexto(negocioA, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Insumos", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("KG", "Kilogramo", true);
            carne = producto(categoria, unidad, "CARNE-1", "Carne de res");
            arroz = producto(categoria, unidad, "ARROZ-1", "Arroz");
            leche = productos.crear(new SolicitudDeProducto("LECHE-1", null, "Leche", null,
                    categoria, unidad, null, null, BigDecimal.ZERO, null, null, null,
                    true, false, true, false, null)).id();
            bodega = bodegas.crear(new SolicitudDeBodega("COC", "Cocina", null, null)).id();
            return null;
        });
    }

    private UUID producto(UUID categoria, UUID unidad, String sku, String nombre) {
        return productos.crear(new SolicitudDeProducto(sku, null, nombre, null, categoria, unidad,
                null, null, BigDecimal.ZERO, null, null, null, false, false, false, false, null))
                .id();
    }

    private void entrar(UUID negocio, UUID productoId, TipoMovimiento tipo, String cantidad,
            String costo) {
        enContexto(negocio, usuario, Set.of(), () -> libro.registrar(new SolicitudDeMovimiento(
                productoId, bodega, tipo, new BigDecimal(cantidad), OrigenMovimiento.COMPRA, null,
                "prueba", UUID.randomUUID().toString(), null, null,
                costo == null ? null : new BigDecimal(costo))));
    }

    private String costoUltimo(UUID productoId) {
        return comoElServicio(negocioA, "select costo_ultimo from productos where id = '"
                + productoId + "'").get(0);
    }

    /** El outbox no tiene RLS: se acota al producto de este test. */
    private List<String> eventosDeCosto() {
        return comoElServicio(negocioA, "select payload::text from outbox_eventos "
                + "where tipo_evento = 'costo_producto_actualizado' and agregado_id = '" + carne
                + "' order by creado_en");
    }

    @Test
    @DisplayName("Criterio 6: una entrada con costo fija el último costo del producto y lo guarda en el movimiento")
    void entradaConCostoFijaElUltimoCosto() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");

        assertThat(costoUltimo(carne)).isEqualTo("28000.0000");
        assertThat(comoElServicio(negocioA, "select costo_unitario from movimientos_inventario "
                + "where producto_id = '" + carne + "'")).containsExactly("28000.0000");
    }

    @Test
    @DisplayName("Criterio 6: la entrada siguiente reemplaza el costo, aunque sea más barata")
    void manda_la_ultima_entrada() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_AJUSTE, "2", "26500");

        assertThat(costoUltimo(carne)).isEqualTo("26500.0000");
    }

    @Test
    @DisplayName("Criterio 6: cada cambio de costo publica costo_producto_actualizado con el costo nuevo")
    void publicaElCambioDeCosto() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");

        assertThat(eventosDeCosto()).singleElement().satisfies(payload -> {
            assertThat(payload).contains("\"negocio_id\": \"" + negocioA + "\"");
            assertThat(payload).contains("\"producto_id\": \"" + carne + "\"");
            assertThat(payload).contains("\"costo_unitario\": 28000");
        });
    }

    @Test
    @DisplayName("Criterio 6: una entrada al mismo costo no vuelve a publicar")
    void mismoCostoNoRepublica() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "5", "28000");

        assertThat(eventosDeCosto()).hasSize(1);
    }

    @Test
    @DisplayName("Criterio 6: una entrada sin costo o una salida no cambian el costo ni publican")
    void sinCostoOSalidaNoCambian() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_DEVOLUCION, "1", null);
        entrar(negocioA, carne, TipoMovimiento.SALIDA_CONSUMO, "3", "99999");

        assertThat(costoUltimo(carne)).isEqualTo("28000.0000");
        assertThat(eventosDeCosto()).hasSize(1);
    }

    @Test
    @DisplayName("Criterio 6: la entrada de un lote con costo fija el último costo del producto")
    void entradaDeLoteConCosto() {
        enContexto(negocioA, usuario, SETUP, () -> lotes.registrarEntrada(
                new SolicitudDeEntradaDeLote(leche, bodega, "L-001", new BigDecimal("20"), null,
                        java.time.LocalDate.now().plusDays(10), null, new BigDecimal("3900"),
                        "compra", UUID.randomUUID().toString())));

        assertThat(costoUltimo(leche)).isEqualTo("3900.0000");
    }

    @Test
    @DisplayName("Criterio 6: un ajuste que suma, con costo, fija el último costo; uno que resta, no")
    void ajusteConCosto() {
        entrar(negocioA, arroz, TipoMovimiento.ENTRADA_COMPRA, "10", "4200");
        UUID suma = enContexto(negocioA, usuario, SETUP, () -> ajustes.crear(new SolicitudDeAjuste(
                "AJ-C1", bodega, com.regenta.inventario.domain.TipoAjuste.CONTEO_FISICO,
                "Conteo", List.of(new LineaDeAjuste(arroz, null, new BigDecimal("12"),
                        new BigDecimal("4500")))))).id();
        enContexto(negocioA, UUID.randomUUID(), SETUP, () -> ajustes.aplicar(suma));

        assertThat(costoUltimo(arroz)).isEqualTo("4500.0000");

        UUID resta = enContexto(negocioA, usuario, SETUP, () -> ajustes.crear(new SolicitudDeAjuste(
                "AJ-C2", bodega, com.regenta.inventario.domain.TipoAjuste.CONTEO_FISICO,
                "Conteo", List.of(new LineaDeAjuste(arroz, null, new BigDecimal("11"),
                        new BigDecimal("1")))))).id();
        enContexto(negocioA, UUID.randomUUID(), SETUP, () -> ajustes.aplicar(resta));

        assertThat(costoUltimo(arroz)).isEqualTo("4500.0000");
    }

    @Test
    @DisplayName("Criterio 6: los costos se consultan por lote de productos; sin costo conocido, no aparece")
    void consultaDeCostos() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");

        Map<UUID, BigDecimal> costos = enContexto(negocioA, usuario, VER,
                () -> productos.costos(List.of(carne, arroz)));

        assertThat(costos).containsOnlyKeys(carne);
        assertThat(costos.get(carne)).isEqualByComparingTo("28000");
    }

    @Test
    @DisplayName("Criterio 9: el negocio B no ve los costos de los productos del negocio A")
    void aislamientoEntreNegocios() {
        entrar(negocioA, carne, TipoMovimiento.ENTRADA_COMPRA, "10", "28000");

        assertThat(enContexto(negocioB, usuario, VER, () -> productos.costos(List.of(carne))))
                .isEmpty();
    }

    @Test
    @DisplayName("Sin permiso de ver productos no se consultan costos")
    void sinPermiso() {
        assertThatThrownBy(() -> enContexto(negocioA, usuario, Set.of(),
                () -> productos.costos(List.of(carne)))).isInstanceOf(SinPermisoException.class);
    }
}
