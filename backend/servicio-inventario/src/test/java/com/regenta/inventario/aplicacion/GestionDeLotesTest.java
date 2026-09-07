package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;

/** HU-031. Lotes y fechas de vencimiento: FEFO y bloqueo del vencido. */
class GestionDeLotesTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_LOTE_REGISTRAR",
            "INVENTARIO_LOTE_VER");

    @Autowired
    private GestionDeLotes lotes;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID perecedero;
    private UUID sinLotes;
    private UUID bodega1;
    private UUID bodega2;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Medicamentos", null, null, null, null, null))
                    .id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            perecedero = productos.crear(new SolicitudDeProducto("MED-1", null, "Amoxicilina", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    true, false, true, false, null)).id();
            sinLotes = productos.crear(new SolicitudDeProducto("TOR-1", null, "Tornillo", null,
                    categoria, unidad, null, null, BigDecimal.ONE, null, null, null,
                    false, false, false, false, null)).id();
            bodega1 = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            bodega2 = bodegas.crear(new SolicitudDeBodega("B2", "Bodega 2", null, null)).id();
            return null;
        });
    }

    private RegistroDeLote entrada(UUID producto, UUID bodega, String codigoLote,
            LocalDate vencimiento, String cantidad, String idem) {
        return enContexto(negocio, usuario, SETUP, () -> lotes.registrarEntrada(
                new SolicitudDeEntradaDeLote(producto, bodega, codigoLote, new BigDecimal(cantidad),
                        null, vencimiento, null, null, "compra", idem)));
    }

    private RegistroDeMovimiento salida(UUID producto, UUID bodega, String codigoLote,
            String cantidad, String autorizacion, String idem) {
        return enContexto(negocio, usuario, SETUP, () -> lotes.registrarSalida(
                new SolicitudDeSalidaDeLote(producto, bodega, codigoLote, new BigDecimal(cantidad),
                        autorizacion, "venta", idem)));
    }

    @Test
    @DisplayName("Criterio 1: un producto que maneja lotes exige el codigo de lote al entrar mercancia")
    void exigeElCodigoDeLoteEnLaEntrada() {
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> lotes.registrarEntrada(
                new SolicitudDeEntradaDeLote(perecedero, bodega1, "  ", new BigDecimal("10"),
                        null, LocalDate.now().plusMonths(6), null, null, "compra", "e-sin-lote"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("lote");

        RegistroDeLote registro = entrada(perecedero, bodega1, "L-2401",
                LocalDate.now().plusMonths(6), "10", "e-ok");

        assertThat(registro.codigoLote()).isEqualTo("L-2401");
        assertThat(comoElServicio(negocio,
                "select codigo_lote from lotes where producto_id = '" + perecedero + "'"))
                .containsExactly("L-2401");
        assertThat(comoElServicio(negocio, "select cantidad from existencias_lote"))
                .containsExactly("10.000000");
        assertThat(comoElServicio(negocio,
                "select lote_id is not null from movimientos_inventario")).containsExactly("t");
    }

    @Test
    @DisplayName("Criterio 1: un producto que no maneja lotes no admite una entrada por lote")
    void elProductoSinLotesNoAdmiteEntradaPorLote() {
        assertThatThrownBy(() -> entrada(sinLotes, bodega1, "L-1", null, "5", "e-no"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no maneja lotes");
    }

    @Test
    @DisplayName("Criterio 2: la existencia de un lote se ve desglosada por bodega")
    void laExistenciaDelLoteSeVePorBodega() {
        entrada(perecedero, bodega1, "L-500", LocalDate.now().plusMonths(3), "8", "e1");
        entrada(perecedero, bodega2, "L-500", LocalDate.now().plusMonths(3), "5", "e2");

        UUID loteId = enContexto(negocio, usuario, SETUP,
                () -> lotes.registrarEntrada(new SolicitudDeEntradaDeLote(perecedero, bodega1,
                        "L-500", new BigDecimal("2"), null, LocalDate.now().plusMonths(3), null,
                        null, "compra", "e3")).loteId());

        ExistenciaDeLote existencia = enContexto(negocio, usuario, SETUP,
                () -> lotes.existenciaDelLote(loteId));

        Map<String, BigDecimal> porBodega = existencia.porBodega().stream()
                .collect(Collectors.toMap(ExistenciaLoteEnBodega::bodegaNombre,
                        ExistenciaLoteEnBodega::cantidad));
        assertThat(porBodega).containsOnlyKeys("Bodega 1", "Bodega 2");
        assertThat(porBodega.get("Bodega 1")).isEqualByComparingTo("10");
        assertThat(porBodega.get("Bodega 2")).isEqualByComparingTo("5");
        assertThat(existencia.total()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("Criterio 3: al vender un perecedero se sugiere el lote de vencimiento mas proximo (FEFO)")
    void sugiereElLoteFefo() {
        entrada(perecedero, bodega1, "LEJANO", LocalDate.now().plusMonths(12), "10", "e-lejano");
        entrada(perecedero, bodega1, "PROXIMO", LocalDate.now().plusMonths(1), "4", "e-proximo");
        entrada(perecedero, bodega1, "MEDIO", LocalDate.now().plusMonths(6), "10", "e-medio");

        SugerenciaFefo sugerencia = enContexto(negocio, usuario, SETUP,
                () -> lotes.sugerirFefo(perecedero, bodega1, new BigDecimal("6")));

        assertThat(sugerencia.asignaciones()).extracting(AsignacionFefo::codigoLote)
                .containsExactly("PROXIMO", "MEDIO");
        assertThat(sugerencia.asignaciones().get(0).cantidadSugerida()).isEqualByComparingTo("4");
        assertThat(sugerencia.asignaciones().get(1).cantidadSugerida()).isEqualByComparingTo("2");
        assertThat(sugerencia.suficiente()).isTrue();
    }

    @Test
    @DisplayName("Criterio 3: un lote vencido no entra en la sugerencia FEFO")
    void elLoteVencidoNoSeSugiere() {
        entrada(perecedero, bodega1, "VENCIDO", LocalDate.now().minusDays(1), "10", "e-vencido");
        entrada(perecedero, bodega1, "VIGENTE", LocalDate.now().plusMonths(2), "10", "e-vigente");

        SugerenciaFefo sugerencia = enContexto(negocio, usuario, SETUP,
                () -> lotes.sugerirFefo(perecedero, bodega1, new BigDecimal("5")));

        assertThat(sugerencia.asignaciones()).extracting(AsignacionFefo::codigoLote)
                .containsExactly("VIGENTE");
    }

    @Test
    @DisplayName("Criterio 4: un lote vencido se bloquea al venderlo salvo autorizacion, que queda registrada")
    void elLoteVencidoSeBloqueaSalvoAutorizacionRegistrada() {
        entrada(perecedero, bodega1, "L-VTO", LocalDate.now().minusDays(2), "10", "e-vto");

        assertThatThrownBy(() -> salida(perecedero, bodega1, "L-VTO", "3", null, "s-sin-aut"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("vencido");
        assertThat(comoElServicio(negocio, "select cantidad from existencias_lote"))
                .containsExactly("10.000000");

        salida(perecedero, bodega1, "L-VTO", "3", "Campana de donacion, VoBo director tecnico",
                "s-con-aut");

        assertThat(comoElServicio(negocio, "select cantidad from existencias_lote"))
                .containsExactly("7.000000");
        assertThat(comoElServicio(negocio,
                "select motivo from autorizaciones_lote_vencido"))
                .containsExactly("Campana de donacion, VoBo director tecnico");
        assertThat(comoElServicio(negocio,
                "select usuario_id from autorizaciones_lote_vencido"))
                .containsExactly(usuario.toString());
    }

    @Test
    @DisplayName("Los lotes de otro negocio no se mezclan")
    void aisladoPorNegocio() {
        entrada(perecedero, bodega1, "L-AISLA", LocalDate.now().plusMonths(4), "9", "e-aisla");

        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from lotes"))
                .containsExactly("0");
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from existencias_lote"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("La salida por FEFO descuenta del lote sugerido y cuadra con el libro")
    void laSalidaDescuentaDelLote() {
        entrada(perecedero, bodega1, "L-CUADRE", LocalDate.now().plusMonths(5), "20", "e-cuadre");

        salida(perecedero, bodega1, "L-CUADRE", "8", null, "s-cuadre");

        assertThat(comoElServicio(negocio, "select cantidad from existencias_lote"))
                .containsExactly("12.000000");
        assertThat(comoElServicio(negocio,
                "select cantidad from existencias where producto_id = '" + perecedero + "'"))
                .containsExactly("12.000000");
        List<String> tipos = comoElServicio(negocio,
                "select tipo from movimientos_inventario order by ocurrido_en");
        assertThat(tipos).containsExactly("ENTRADA_COMPRA", "SALIDA_VENTA");
    }
}
