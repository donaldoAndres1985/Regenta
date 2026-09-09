package com.regenta.menu.aplicacion;

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

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.menu.BaseDeMenu;
import com.regenta.menu.infra.CostosDeInventarioStub;

/** HU-079. La receta de un ítem y el recálculo de su costo estimado, con dos negocios. */
class GestionDeRecetasTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private GestionDeRecetas recetas;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeItemsDeMenu items;
    @Autowired
    private CostosDeInventarioStub costos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        costos.reiniciar();
    }

    private UUID item(UUID negocio, String codigo) {
        UUID carta = enContexto(negocio, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta " + codigo, null, null, null, null, null, null, false, null))).id();
        UUID cat = enContexto(negocio, admin, ADMIN, () -> cartas.crearCategoria(carta,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        return enContexto(negocio, admin, ADMIN, () -> items.crear(new SolicitudDeItem(cat, null,
                codigo, "Ítem " + codigo, null, "PLATO", new BigDecimal("30000"), null, true, null,
                "FUERTE", null, null, 0))).id();
    }

    private RecetaDelItem agregar(UUID negocio, UUID itemId, UUID productoId, String cantidad,
            String merma) {
        return enContexto(negocio, admin, ADMIN, () -> recetas.agregarLinea(itemId,
                new SolicitudDeLineaDeReceta(productoId, "Insumo", new BigDecimal(cantidad), "kg",
                        new BigDecimal(merma), false)));
    }

    @Test
    @DisplayName("Criterio 1: la receta asocia productos con su cantidad y su merma")
    void definirReceta() {
        UUID it = item(negocioA, "PLATO-1");
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        agregar(negocioA, it, a, "0.25", "0.10");
        RecetaDelItem receta = agregar(negocioA, it, b, "1", "0");

        assertThat(receta.lineas()).hasSize(2)
                .anySatisfy(l -> {
                    assertThat(l.productoId()).isEqualTo(a);
                    assertThat(l.mermaPct()).isEqualByComparingTo("0.10");
                    assertThat(l.cantidadConMerma()).isEqualByComparingTo("0.275");
                });
    }

    @Test
    @DisplayName("Criterio 2: el costo estimado del ítem se recalcula con los costos de inventario")
    void costoSeRecalcula() {
        UUID it = item(negocioA, "PLATO-1");
        UUID carne = UUID.randomUUID();
        UUID arroz = UUID.randomUUID();
        costos.cargarCosto(carne, new BigDecimal("40000"));
        costos.cargarCosto(arroz, new BigDecimal("2000"));

        agregar(negocioA, it, carne, "0.25", "0.10"); // 0.275 * 40000 = 11000
        RecetaDelItem tras = agregar(negocioA, it, arroz, "1", "0"); // + 2000
        assertThat(tras.costoEstimado()).isEqualByComparingTo("13000");

        costos.cargarCosto(carne, new BigDecimal("50000"));
        enContexto(negocioA, admin, ADMIN,
                () -> recetas.recalcularItemsQueUsan(negocioA, carne));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> recetas.ver(it)).costoEstimado())
                .isEqualByComparingTo("15750"); // 0.275 * 50000 + 2000
    }

    @Test
    @DisplayName("Criterio 5: un ítem sin receta no tiene líneas y su costo estimado es cero")
    void itemSinReceta() {
        UUID it = item(negocioA, "PLATO-1");
        RecetaDelItem receta = enContexto(negocioA, admin, ADMIN, () -> recetas.ver(it));
        assertThat(receta.lineas()).isEmpty();
        assertThat(receta.costoEstimado()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El mismo insumo dos veces en la receta responde 409")
    void insumoRepetido() {
        UUID it = item(negocioA, "PLATO-1");
        UUID prod = UUID.randomUUID();
        agregar(negocioA, it, prod, "1", "0");
        assertThatThrownBy(() -> agregar(negocioA, it, prod, "2", "0"))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Quitar una línea recalcula el costo; reemplazar la receta también")
    void quitarYReemplazar() {
        UUID it = item(negocioA, "PLATO-1");
        UUID a = UUID.randomUUID();
        costos.cargarCosto(a, new BigDecimal("10000"));
        UUID lineaId = agregar(negocioA, it, a, "1", "0").lineas().get(0).id();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> recetas.ver(it)).costoEstimado())
                .isEqualByComparingTo("10000");

        enContexto(negocioA, admin, ADMIN, () -> recetas.quitarLinea(lineaId));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> recetas.ver(it)).costoEstimado())
                .isEqualByComparingTo("0");

        UUID b = UUID.randomUUID();
        costos.cargarCosto(b, new BigDecimal("3000"));
        RecetaDelItem tras = enContexto(negocioA, admin, ADMIN, () -> recetas.reemplazar(it,
                List.of(new SolicitudDeLineaDeReceta(b, "B", new BigDecimal("2"), "u",
                        BigDecimal.ZERO, false))));
        assertThat(tras.costoEstimado()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("El segundo negocio no ve la receta del primero")
    void aislamiento() {
        UUID it = item(negocioA, "PLATO-1");
        agregar(negocioA, it, UUID.randomUUID(), "1", "0");
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> recetas.ver(it)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
