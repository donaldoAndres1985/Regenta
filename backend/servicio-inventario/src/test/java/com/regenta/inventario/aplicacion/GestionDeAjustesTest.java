package com.regenta.inventario.aplicacion;

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

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoAjuste;
import com.regenta.inventario.domain.TipoMovimiento;

/** HU-033. Ajustes de inventario con motivo: conteo físico, merma, avería. */
class GestionDeAjustesTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_AJUSTE_CREAR",
            "INVENTARIO_AJUSTE_APLICAR", "INVENTARIO_AJUSTE_VER");

    @Autowired
    private GestionDeAjustes ajustes;

    @Autowired
    private LibroMayorDeInventario libro;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID quienCarga = UUID.randomUUID();
    private final UUID quienAprueba = UUID.randomUUID();

    private UUID producto;
    private UUID bodega;

    @BeforeEach
    void preparar() {
        enContexto(negocio, quienCarga, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-A", null, "Prod", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    false, false, false, false, null)).id();
            bodega = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            libro.registrar(new SolicitudDeMovimiento(producto, bodega, TipoMovimiento.ENTRADA_COMPRA,
                    new BigDecimal("100"), OrigenMovimiento.CARGA_INICIAL, null, "carga", "carga-1"));
            return null;
        });
    }

    private UUID crear(String numero, TipoAjuste tipo, String motivo, String cantidadFisica) {
        return enContexto(negocio, quienCarga, SETUP, () -> ajustes.crear(new SolicitudDeAjuste(
                numero, bodega, tipo, motivo,
                List.of(new LineaDeAjuste(producto, null, new BigDecimal(cantidadFisica), null))))
                .id());
    }

    private String cantidadExistencia() {
        return comoElServicio(negocio, "select cantidad from existencias where producto_id = '"
                + producto + "' and bodega_id = '" + bodega + "'").stream().findFirst().orElse("0");
    }

    @Test
    @DisplayName("Criterio 1: al cargar el ajuste se registran sistema y física, y la diferencia se calcula sola")
    void registraSistemaFisicaYLaDiferenciaSeCalculaSola() {
        UUID a = crear("AJ-1", TipoAjuste.CONTEO_FISICO, "Conteo trimestral", "93");

        assertThat(comoElServicio(negocio, "select cantidad_sistema from ajuste_lineas where ajuste_id = '"
                + a + "'")).containsExactly("100.000000");
        assertThat(comoElServicio(negocio, "select cantidad_fisica from ajuste_lineas where ajuste_id = '"
                + a + "'")).containsExactly("93.000000");
        assertThat(comoElServicio(negocio, "select diferencia from ajuste_lineas where ajuste_id = '"
                + a + "'")).containsExactly("-7.000000");
        // Sigue en borrador: todavía no tocó el stock.
        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("100");
        assertThat(comoElServicio(negocio, "select estado from ajustes_inventario where id = '" + a + "'"))
                .containsExactly("BORRADOR");
    }

    @Test
    @DisplayName("Criterio 2: al aplicar se generan los movimientos y ya no se puede volver a aplicar")
    void alAplicarSeGeneranLosMovimientosYNoSePuedeReaplicar() {
        UUID a = crear("AJ-2", TipoAjuste.MERMA, "Producto derramado", "88");
        enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a));

        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("88");
        assertThat(comoElServicio(negocio, "select tipo from movimientos_inventario "
                + "where origen_tipo = 'AJUSTE' and origen_id = '" + a + "'"))
                .containsExactly("SALIDA_AJUSTE");
        assertThat(comoElServicio(negocio, "select cantidad from movimientos_inventario "
                + "where origen_id = '" + a + "'")).containsExactly("12.000000");
        assertThat(comoElServicio(negocio, "select estado from ajustes_inventario where id = '" + a + "'"))
                .containsExactly("APLICADO");

        assertThatThrownBy(() -> enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 2: un ajuste positivo genera una entrada")
    void unAjustePositivoGeneraUnaEntrada() {
        UUID a = crear("AJ-3", TipoAjuste.CONTEO_FISICO, "Sobrante en conteo", "105");
        enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a));

        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("105");
        assertThat(comoElServicio(negocio, "select tipo from movimientos_inventario "
                + "where origen_id = '" + a + "'")).containsExactly("ENTRADA_AJUSTE");
    }

    @Test
    @DisplayName("Criterio 3: aplicar un ajuste sin motivo se rechaza")
    void aplicarSinMotivoSeRechaza() {
        UUID a = crear("AJ-4", TipoAjuste.AVERIA, "  ", "90");

        assertThatThrownBy(() -> enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("motivo");
        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("100");
        assertThat(comoElServicio(negocio, "select estado from ajustes_inventario where id = '" + a + "'"))
                .containsExactly("BORRADOR");
    }

    @Test
    @DisplayName("Criterio 4: en el ajuste aplicado consta quién lo cargó y quién lo aprobó")
    void constaQuienCargoYQuienAprobo() {
        UUID a = crear("AJ-5", TipoAjuste.MERMA, "Vencimiento", "80");
        enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a));

        assertThat(comoElServicio(negocio, "select usuario_id from ajustes_inventario where id = '"
                + a + "'")).containsExactly(quienCarga.toString());
        assertThat(comoElServicio(negocio, "select aprobado_por from ajustes_inventario where id = '"
                + a + "'")).containsExactly(quienAprueba.toString());
    }

    @Test
    @DisplayName("Un ajuste sin diferencia no genera movimientos pero queda aplicado")
    void ajusteSinDiferencia() {
        UUID a = crear("AJ-6", TipoAjuste.CONTEO_FISICO, "Todo cuadra", "100");
        enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a));

        assertThat(comoElServicio(negocio, "select count(*) from movimientos_inventario "
                + "where origen_id = '" + a + "'")).containsExactly("0");
        assertThat(comoElServicio(negocio, "select estado from ajustes_inventario where id = '" + a + "'"))
                .containsExactly("APLICADO");
    }

    @Test
    @DisplayName("El libro cuadra con las existencias tras aplicar el ajuste")
    void elLibroCuadra() {
        UUID a = crear("AJ-7", TipoAjuste.MERMA, "Rotura", "73");
        enContexto(negocio, quienAprueba, SETUP, () -> ajustes.aplicar(a));

        BigDecimal segunLibro = enContexto(negocio, quienAprueba, SETUP,
                () -> libro.saldoSegunElLibro(producto, bodega));
        assertThat(segunLibro).isEqualByComparingTo("73");
        assertThat(new BigDecimal(cantidadExistencia())).isEqualByComparingTo("73");
    }

    @Test
    @DisplayName("Un número de ajuste repetido en el negocio se rechaza")
    void numeroRepetidoSeRechaza() {
        crear("AJ-8", TipoAjuste.OTRO, "x", "99");
        assertThatThrownBy(() -> crear("AJ-8", TipoAjuste.OTRO, "y", "98"))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Los ajustes de otro negocio no se mezclan")
    void aisladoPorNegocio() {
        crear("AJ-9", TipoAjuste.MERMA, "x", "90");
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from ajustes_inventario"))
                .containsExactly("0");
    }
}
