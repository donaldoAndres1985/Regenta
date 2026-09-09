package com.regenta.menu.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.menu.BaseDeMenu;
import com.regenta.menu.aplicacion.CartaDeMenu.CategoriaConItems;

/** HU-077. Ítems de menú, con dos negocios cargados. */
class GestionDeItemsDeMenuTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR", "MENU_PLATO_ELIMINAR");

    @Autowired
    private GestionDeItemsDeMenu items;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeEstacionesDeCocina estaciones;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID carta(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                nombre, null, null, null, null, null, null, false, null))).id();
    }

    private UUID categoria(UUID negocio, UUID cartaId, String nombre, int orden) {
        return enContexto(negocio, admin, ADMIN, () -> cartas.crearCategoria(cartaId,
                new SolicitudDeCategoria(nombre, null, orden, null))).id();
    }

    private UUID estacion(UUID negocio, String codigo) {
        return enContexto(negocio, admin, ADMIN, () -> estaciones.crear(
                new SolicitudDeEstacion(codigo, "Estación " + codigo, null, 0, null))).id();
    }

    private ItemDelNegocio crearItem(UUID negocio, UUID categoriaId, UUID estacionId, String codigo,
            String precio, Integer tiempo, Map<String, Object> atributos) {
        return enContexto(negocio, admin, ADMIN, () -> items.crear(new SolicitudDeItem(categoriaId,
                estacionId, codigo, "Ítem " + codigo, null, "PLATO", new BigDecimal(precio), null,
                true, tiempo, "FUERTE", atributos, null, 0)));
    }

    @Test
    @DisplayName("Criterio 1: un ítem se crea con categoría, precio, estación y tiempo de preparación")
    void crearItemCompleto() {
        UUID c = carta(negocioA, "Principal");
        UUID cat = categoria(negocioA, c, "Fuertes", 0);
        UUID est = estacion(negocioA, "PARRILLA");

        ItemDelNegocio item = crearItem(negocioA, cat, est, "PLATO-1", "32000", 18, null);

        assertThat(item.categoriaMenuId()).isEqualTo(cat);
        assertThat(item.estacionId()).isEqualTo(est);
        assertThat(item.precio()).isEqualByComparingTo("32000");
        assertThat(item.tiempoPreparacionMin()).isEqualTo(18);
        assertThat(item.disponible()).isTrue();
    }

    @Test
    @DisplayName("Criterio 2: un código de ítem repetido en el negocio responde 409")
    void codigoRepetido() {
        UUID catA = categoria(negocioA, carta(negocioA, "A"), "Fuertes", 0);
        crearItem(negocioA, catA, null, "BURGER", "25000", null, null);

        assertThatThrownBy(() -> crearItem(negocioA, catA, null, "BURGER", "27000", null, null))
                .isInstanceOf(RecursoDuplicadoException.class);
        // El mismo código en otro negocio sí se puede.
        UUID catB = categoria(negocioB, carta(negocioB, "B"), "Fuertes", 0);
        assertThat(crearItem(negocioB, catB, null, "BURGER", "20000", null, null).id()).isNotNull();
    }

    @Test
    @DisplayName("Criterio 3: los atributos (alérgenos, vegano, picante) quedan en el JSONB")
    void atributosEnJsonb() {
        UUID cat = categoria(negocioA, carta(negocioA, "Principal"), "Fuertes", 0);
        UUID id = crearItem(negocioA, cat, null, "PASTA-1", "28000", null,
                Map.of("alergenos", List.of("gluten", "lacteos"), "vegano", false, "picante", 1))
                .id();

        assertThat(enContexto(negocioA, admin, ADMIN, () -> items.ver(id)).atributos())
                .containsEntry("vegano", false).containsEntry("picante", 1);
        assertThat(consultar("SELECT atributos FROM items_menu WHERE id = '" + id + "'").get(0))
                .contains("gluten").contains("lacteos");
    }

    @Test
    @DisplayName("Criterio 4: un ítem agotado sigue en la carta del mesero pero no se puede pedir")
    void agotadoEnLaCartaDelMesero() {
        UUID c = carta(negocioA, "Principal");
        UUID cat = categoria(negocioA, c, "Fuertes", 0);
        UUID id = crearItem(negocioA, cat, null, "PLATO-1", "32000", null, null).id();

        assertThat(itemDelMenu(negocioA, c, id).pedible()).isTrue();

        enContexto(negocioA, admin, ADMIN, () -> items.cambiarDisponibilidad(id, false));
        ItemEnCarta agotado = itemDelMenu(negocioA, c, id);
        assertThat(agotado.disponible()).isFalse();
        assertThat(agotado.pedible()).isFalse();
    }

    @Test
    @DisplayName("Un ítem eliminado desaparece de la carta y del listado")
    void eliminadoDesaparece() {
        UUID c = carta(negocioA, "Principal");
        UUID cat = categoria(negocioA, c, "Fuertes", 0);
        UUID id = crearItem(negocioA, cat, null, "PLATO-1", "32000", null, null).id();

        enContexto(negocioA, admin, ADMIN, () -> items.eliminar(id));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> items.menuDeCarta(c)).categorias())
                .flatExtracting(CategoriaConItems::items).isEmpty();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> items.listarPorCategoria(cat))).isEmpty();
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> items.ver(id)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Una estación o una categoría inexistente responde 404")
    void referenciasInexistentes() {
        UUID cat = categoria(negocioA, carta(negocioA, "Principal"), "Fuertes", 0);

        assertThatThrownBy(() -> crearItem(negocioA, cat, UUID.randomUUID(), "X-1", "1000", null,
                null)).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> crearItem(negocioA, UUID.randomUUID(), null, "X-2", "1000", null,
                null)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve los ítems del primero")
    void aislamientoEntreNegocios() {
        UUID cat = categoria(negocioA, carta(negocioA, "Principal"), "Fuertes", 0);
        UUID id = crearItem(negocioA, cat, null, "PLATO-1", "32000", null, null).id();

        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> items.ver(id)))
                .isInstanceOf(NoEncontradoException.class);
    }

    private ItemEnCarta itemDelMenu(UUID negocio, UUID cartaId, UUID itemId) {
        return enContexto(negocio, admin, ADMIN, () -> items.menuDeCarta(cartaId)).categorias()
                .stream().flatMap(cc -> cc.items().stream())
                .filter(it -> it.id().equals(itemId)).findFirst().orElseThrow();
    }
}
