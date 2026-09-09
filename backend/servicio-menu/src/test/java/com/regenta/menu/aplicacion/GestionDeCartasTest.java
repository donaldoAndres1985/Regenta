package com.regenta.menu.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.menu.BaseDeMenu;

/** HU-076. Cartas por horario y sus categorías, con dos negocios cargados. */
class GestionDeCartasTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR", "MENU_PLATO_ELIMINAR");

    @Autowired
    private GestionDeCartas cartas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private CartaDelNegocio crear(UUID negocio, String nombre, LocalTime desde, LocalTime hasta,
            boolean esDefault) {
        return enContexto(negocio, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                nombre, null, desde, hasta, null, null, null, esDefault, null)));
    }

    private UUID categoria(UUID negocio, UUID cartaId, String nombre, int orden) {
        return enContexto(negocio, admin, ADMIN, () -> cartas.crearCategoria(cartaId,
                new SolicitudDeCategoria(nombre, null, orden, null))).id();
    }

    @Test
    @DisplayName("Criterio 1: una carta de desayunos no aparece entre las disponibles a las 15:00")
    void cartaFueraDeHorario() {
        UUID desayunos = crear(negocioA, "Desayunos", LocalTime.of(6, 0), LocalTime.of(11, 0), false)
                .id();
        crear(negocioA, "Principal", null, null, true);

        List<CartaDelNegocio> alMediodia = enContexto(negocioA, admin, ADMIN,
                () -> cartas.disponibles(OffsetDateTime.parse("2026-05-04T15:00:00Z")));
        assertThat(alMediodia).extracting(CartaDelNegocio::nombre).containsExactly("Principal");

        List<CartaDelNegocio> aLasOcho = enContexto(negocioA, admin, ADMIN,
                () -> cartas.disponibles(OffsetDateTime.parse("2026-05-04T08:00:00Z")));
        assertThat(aLasOcho).extracting(CartaDelNegocio::id)
                .containsExactlyInAnyOrder(desayunos,
                        enContexto(negocioA, admin, ADMIN, () -> cartas.listar()).stream()
                                .filter(c -> c.nombre().equals("Principal")).findFirst()
                                .orElseThrow().id());
    }

    @Test
    @DisplayName("Criterio 2: las categorías se devuelven en el orden definido")
    void categoriasEnOrden() {
        UUID carta = crear(negocioA, "Principal", null, null, true).id();
        UUID fuertes = categoria(negocioA, carta, "Fuertes", 2);
        UUID entradas = categoria(negocioA, carta, "Entradas", 0);
        UUID bebidas = categoria(negocioA, carta, "Bebidas", 1);

        assertThat(enContexto(negocioA, admin, ADMIN, () -> cartas.ver(carta)).categorias())
                .extracting(CategoriaDelNegocio::id).containsExactly(entradas, bebidas, fuertes);

        enContexto(negocioA, admin, ADMIN,
                () -> cartas.reordenarCategorias(carta, List.of(bebidas, fuertes, entradas)));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> cartas.ver(carta)).categorias())
                .extracting(CategoriaDelNegocio::id).containsExactly(bebidas, fuertes, entradas);
    }

    @Test
    @DisplayName("Criterio 3: una carta con platos no se puede borrar")
    void noBorrarCartaConItems() {
        UUID carta = crear(negocioA, "Principal", null, null, true).id();
        UUID cat = categoria(negocioA, carta, "Fuertes", 0);
        UUID item = UUID.randomUUID();
        ejecutarComoElServicio(negocioA, "INSERT INTO items_menu "
                + "(id, negocio_id, categoria_menu_id, codigo, nombre, precio) VALUES ('" + item
                + "', '" + negocioA + "', '" + cat + "', 'PLATO-1', 'Bandeja paisa', 32000)");

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> cartas.eliminar(carta)))
                .isInstanceOf(ConflictoDeEstadoException.class);

        ejecutarComoElServicio(negocioA, "DELETE FROM items_menu WHERE id = '" + item + "'");
        assertThatCode(() -> enContexto(negocioA, admin, ADMIN, () -> cartas.eliminar(carta)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> cartas.ver(carta)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Solo una carta es la de por defecto")
    void soloUnaDefault() {
        UUID primera = crear(negocioA, "Primera", null, null, true).id();
        UUID segunda = crear(negocioA, "Segunda", null, null, true).id();

        assertThat(enContexto(negocioA, admin, ADMIN, () -> cartas.listar()))
                .filteredOn(CartaDelNegocio::esDefault).extracting(CartaDelNegocio::id)
                .containsExactly(segunda);

        enContexto(negocioA, admin, ADMIN, () -> cartas.marcarPorDefecto(primera));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> cartas.listar()))
                .filteredOn(CartaDelNegocio::esDefault).extracting(CartaDelNegocio::id)
                .containsExactly(primera);
    }

    @Test
    @DisplayName("Una carta desactivada sale de las disponibles pero sigue en el listado")
    void desactivarSacaDeDisponibles() {
        UUID carta = crear(negocioA, "Principal", null, null, true).id();
        enContexto(negocioA, admin, ADMIN, () -> cartas.cambiarActivacion(carta, false));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> cartas.disponibles(null))).isEmpty();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> cartas.listar()))
                .extracting(CartaDelNegocio::id).contains(carta);
    }

    @Test
    @DisplayName("El segundo negocio no ve las cartas del primero")
    void aislamientoEntreNegocios() {
        UUID enA = crear(negocioA, "Principal A", null, null, true).id();
        crear(negocioB, "Principal B", null, null, true);

        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> cartas.ver(enA)))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(enContexto(negocioB, admin, ADMIN, () -> cartas.listar()))
                .extracting(CartaDelNegocio::nombre).containsExactly("Principal B");
    }
}
