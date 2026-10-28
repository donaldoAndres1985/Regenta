package com.regenta.inventario.aplicacion;

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

import com.regenta.comun.errores.SinPermisoException;
import com.regenta.inventario.BaseDeInventario;

/**
 * HU-138 criterio 7, lado proveedor. Los lotes con saldo que vencen dentro de
 * una ventana de días: lo que el barrido de alertas necesita para avisar de
 * vencimientos (HU-093 criterio 2). Con dos negocios cargados.
 */
class LotesPorVencerTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_LOTE_REGISTRAR",
            "INVENTARIO_LOTE_VER");
    private static final Set<String> VER = Set.of("INVENTARIO_LOTE_VER");
    private static final LocalDate HOY = LocalDate.now();

    @Autowired
    private GestionDeLotes lotes;
    @Autowired
    private GestionDeCategorias categorias;
    @Autowired
    private GestionDeProductos productos;
    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID yogur;
    private UUID bodega1;
    private UUID bodega2;

    @BeforeEach
    void preparar() {
        enContexto(negocioA, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Lácteos", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            yogur = productos.crear(new SolicitudDeProducto("YOG-1", null, "Yogur", null,
                    categoria, unidad, null, null, BigDecimal.ONE, null, null, null,
                    true, false, true, false, null)).id();
            bodega1 = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            bodega2 = bodegas.crear(new SolicitudDeBodega("B2", "Bodega 2", null, null)).id();
            return null;
        });
    }

    private void entrada(UUID bodega, String codigo, LocalDate vence, String cantidad) {
        enContexto(negocioA, usuario, SETUP, () -> lotes.registrarEntrada(
                new SolicitudDeEntradaDeLote(yogur, bodega, codigo, new BigDecimal(cantidad), null,
                        vence, null, null, "compra", UUID.randomUUID().toString())));
    }

    private void salida(UUID bodega, String codigo, String cantidad) {
        enContexto(negocioA, usuario, SETUP, () -> lotes.registrarSalida(
                new SolicitudDeSalidaDeLote(yogur, bodega, codigo, new BigDecimal(cantidad), null,
                        "venta", UUID.randomUUID().toString())));
    }

    @Test
    @DisplayName("Criterio 7: un lote con saldo que vence dentro de la ventana aparece, con sus días y su existencia total")
    void loteDentroDeLaVentana() {
        entrada(bodega1, "L-10", HOY.plusDays(10), "6");
        entrada(bodega2, "L-10", HOY.plusDays(10), "4");

        var porVencer = enContexto(negocioA, usuario, VER, () -> lotes.porVencer(30));

        assertThat(porVencer).singleElement().satisfies(l -> {
            assertThat(l.productoId()).isEqualTo(yogur);
            assertThat(l.productoNombre()).isEqualTo("Yogur");
            assertThat(l.codigoLote()).isEqualTo("L-10");
            assertThat(l.fechaVencimiento()).isEqualTo(HOY.plusDays(10));
            assertThat(l.diasParaVencer()).isEqualTo(10);
            assertThat(l.existencia()).isEqualByComparingTo("10");
        });
    }

    @Test
    @DisplayName("Criterio 7: quedan fuera los que vencen después de la ventana, los ya vencidos y los sin saldo")
    void fueraDeLaVentana() {
        entrada(bodega1, "L-LEJOS", HOY.plusDays(40), "5");
        entrada(bodega1, "L-AGOTADO", HOY.plusDays(5), "2");
        salida(bodega1, "L-AGOTADO", "2");
        entrada(bodega1, "L-HOY", HOY, "1");

        var porVencer = enContexto(negocioA, usuario, VER, () -> lotes.porVencer(30));

        assertThat(porVencer).extracting(LotePorVencer::codigoLote).containsExactly("L-HOY");
        assertThat(porVencer.get(0).diasParaVencer()).isZero();
    }

    @Test
    @DisplayName("Criterio 7: ordenados del que vence antes al que vence después")
    void ordenados() {
        entrada(bodega1, "L-20", HOY.plusDays(20), "1");
        entrada(bodega1, "L-03", HOY.plusDays(3), "1");

        assertThat(enContexto(negocioA, usuario, VER, () -> lotes.porVencer(30)))
                .extracting(LotePorVencer::codigoLote).containsExactly("L-03", "L-20");
    }

    @Test
    @DisplayName("Criterio 9: el negocio B no ve los lotes del negocio A")
    void aislamientoEntreNegocios() {
        entrada(bodega1, "L-A", HOY.plusDays(5), "3");

        assertThat(enContexto(negocioA, usuario, VER, () -> lotes.porVencer(30))).hasSize(1);
        assertThat(enContexto(negocioB, usuario, VER, () -> lotes.porVencer(30))).isEmpty();
    }

    @Test
    @DisplayName("Sin permiso de ver lotes no se consulta")
    void sinPermiso() {
        assertThatThrownBy(() -> enContexto(negocioA, usuario, Set.of(),
                () -> lotes.porVencer(30))).isInstanceOf(SinPermisoException.class);
    }
}
