package com.regenta.menu.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.menu.BaseDeMenu;

/** HU-079. La explosión de líneas de comanda contra sus recetas y modificadores. */
class ExplosionDeRecetasTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private ExplosionDeRecetas explosion;
    @Autowired
    private GestionDeRecetas recetas;
    @Autowired
    private GestionDeModificadores modificadores;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeItemsDeMenu items;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID carne = UUID.randomUUID();
    private final UUID arroz = UUID.randomUUID();

    private UUID itemConReceta(String codigo, boolean conReceta) {
        UUID carta = enContexto(negocioA, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta " + codigo, null, null, null, null, null, null, false, null))).id();
        UUID cat = enContexto(negocioA, admin, ADMIN, () -> cartas.crearCategoria(carta,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        UUID it = enContexto(negocioA, admin, ADMIN, () -> items.crear(new SolicitudDeItem(cat,
                null, codigo, "Ítem " + codigo, null, "PLATO", new BigDecimal("30000"), null, true,
                null, "FUERTE", null, null, 0))).id();
        if (conReceta) {
            enContexto(negocioA, admin, ADMIN, () -> recetas.agregarLinea(it,
                    new SolicitudDeLineaDeReceta(carne, "Carne", new BigDecimal("0.2"), "kg",
                            BigDecimal.ZERO, false)));
            enContexto(negocioA, admin, ADMIN, () -> recetas.agregarLinea(it,
                    new SolicitudDeLineaDeReceta(arroz, "Arroz", new BigDecimal("0.15"), "kg",
                            BigDecimal.ZERO, false)));
        }
        return it;
    }

    private List<InsumoAConsumir> explotar(List<LineaAExplotar> lineas) {
        return enContexto(negocioA, admin, ADMIN, () -> explosion.explotar(lineas));
    }

    @Test
    @DisplayName("Vender 3 bandejas descuenta exactamente 3 veces la receta")
    void tresBandejas() {
        UUID bandeja = itemConReceta("BANDEJA", true);

        List<InsumoAConsumir> insumos = explotar(List.of(
                new LineaAExplotar(bandeja, new BigDecimal("3"), List.of())));

        assertThat(insumos).hasSize(2);
        assertThat(insumos).anySatisfy(i -> {
            assertThat(i.productoId()).isEqualTo(carne);
            assertThat(i.cantidad()).isEqualByComparingTo("0.6");
        });
        assertThat(insumos).anySatisfy(i -> {
            assertThat(i.productoId()).isEqualTo(arroz);
            assertThat(i.cantidad()).isEqualByComparingTo("0.45");
        });
    }

    @Test
    @DisplayName("Criterio 4: un modificador enlazado a un insumo también se descuenta")
    void modificadorDescuentaInsumo() {
        UUID bandeja = itemConReceta("BANDEJA", true);
        UUID queso = UUID.randomUUID();
        UUID grupo = enContexto(negocioA, admin, ADMIN,
                () -> modificadores.crearGrupo(new SolicitudDeGrupo("Extras", 0, 3))).id();
        UUID extraQueso = enContexto(negocioA, admin, ADMIN, () -> modificadores.agregarModificador(
                grupo, new SolicitudDeModificador("Extra queso", new BigDecimal("3000"), queso,
                        new BigDecimal("0.05"), 0))).id();
        enContexto(negocioA, admin, ADMIN, () -> modificadores.vincular(bandeja, grupo, 0));

        List<InsumoAConsumir> insumos = explotar(List.of(
                new LineaAExplotar(bandeja, new BigDecimal("2"), List.of(extraQueso))));

        assertThat(insumos).anySatisfy(i -> {
            assertThat(i.productoId()).isEqualTo(queso);
            assertThat(i.cantidad()).isEqualByComparingTo("0.10");
        });
        assertThat(insumos).anySatisfy(i -> {
            assertThat(i.productoId()).isEqualTo(carne);
            assertThat(i.cantidad()).isEqualByComparingTo("0.4");
        });
    }

    @Test
    @DisplayName("Criterio 5: un ítem sin receta no aporta insumos y no falla")
    void itemSinReceta() {
        UUID sinReceta = itemConReceta("BEBIDA", false);
        assertThat(explotar(List.of(new LineaAExplotar(sinReceta, new BigDecimal("5"), List.of()))))
                .isEmpty();
    }

    @Test
    @DisplayName("Varias líneas del mismo ítem se suman por producto")
    void lineasSeAgrupan() {
        UUID bandeja = itemConReceta("BANDEJA", true);

        List<InsumoAConsumir> insumos = explotar(List.of(
                new LineaAExplotar(bandeja, BigDecimal.ONE, List.of()),
                new LineaAExplotar(bandeja, new BigDecimal("2"), List.of())));

        assertThat(insumos).filteredOn(i -> i.productoId().equals(carne)).singleElement()
                .satisfies(i -> assertThat(i.cantidad()).isEqualByComparingTo("0.6"));
    }
}
