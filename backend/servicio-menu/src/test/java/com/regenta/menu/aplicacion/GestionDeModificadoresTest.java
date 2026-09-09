package com.regenta.menu.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.menu.BaseDeMenu;

/** HU-078. Grupos de modificadores, sus mínimos y máximos, y la cotización de una selección. */
class GestionDeModificadoresTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR", "MENU_PLATO_ELIMINAR");

    @Autowired
    private GestionDeModificadores modificadores;
    @Autowired
    private GestionDeCartas cartas;
    @Autowired
    private GestionDeItemsDeMenu items;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID item(UUID negocio, String codigo) {
        UUID carta = enContexto(negocio, admin, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta " + codigo, null, null, null, null, null, null, false, null))).id();
        UUID cat = enContexto(negocio, admin, ADMIN, () -> cartas.crearCategoria(carta,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        return enContexto(negocio, admin, ADMIN, () -> items.crear(new SolicitudDeItem(cat, null,
                codigo, "Ítem " + codigo, null, "PLATO", new BigDecimal("30000"), null, true, null,
                "FUERTE", null, null, 0))).id();
    }

    private UUID grupo(UUID negocio, String nombre, int min, int max) {
        return enContexto(negocio, admin, ADMIN,
                () -> modificadores.crearGrupo(new SolicitudDeGrupo(nombre, min, max))).id();
    }

    private UUID opcion(UUID negocio, UUID grupoId, String nombre, String precioExtra) {
        return enContexto(negocio, admin, ADMIN, () -> modificadores.agregarModificador(grupoId,
                new SolicitudDeModificador(nombre, new BigDecimal(precioExtra), null, null, 0)))
                .id();
    }

    private void vincular(UUID negocio, UUID itemId, UUID grupoId) {
        enContexto(negocio, admin, ADMIN, () -> modificadores.vincular(itemId, grupoId, 0));
    }

    private CotizacionDeModificadores cotizar(UUID negocio, UUID itemId, List<UUID> ids) {
        return enContexto(negocio, admin, ADMIN,
                () -> modificadores.cotizarSeleccion(itemId, ids));
    }

    @Test
    @DisplayName("Criterio 4: la base rechaza un grupo con máximo menor que el mínimo")
    void maxMenorQueMinLoRechazaLaBase() {
        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA, "INSERT INTO grupos_modificadores "
                + "(id, negocio_id, nombre, min_selecciones, max_selecciones) VALUES ('"
                + UUID.randomUUID() + "', '" + negocioA + "', 'Malo', 3, 1)"))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> grupo(negocioA, "Malo", 3, 1))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 1: un grupo obligatorio sin elegir nada rechaza la selección")
    void grupoObligatorioSinElegir() {
        UUID it = item(negocioA, "PLATO-1");
        UUID g = grupo(negocioA, "Término de la carne", 1, 1);
        opcion(negocioA, g, "Término medio", "0");
        opcion(negocioA, g, "Bien cocida", "0");
        vincular(negocioA, it, g);

        assertThatThrownBy(() -> cotizar(negocioA, it, List.of()))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 2: elegir más opciones que el máximo del grupo rechaza la selección")
    void masQueElMaximo() {
        UUID it = item(negocioA, "PLATO-1");
        UUID g = grupo(negocioA, "Salsas", 0, 5);
        List<UUID> seis = List.of(
                opcion(negocioA, g, "Salsa 1", "0"), opcion(negocioA, g, "Salsa 2", "0"),
                opcion(negocioA, g, "Salsa 3", "0"), opcion(negocioA, g, "Salsa 4", "0"),
                opcion(negocioA, g, "Salsa 5", "0"), opcion(negocioA, g, "Salsa 6", "0"));
        vincular(negocioA, it, g);

        assertThatThrownBy(() -> cotizar(negocioA, it, seis))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 3: un modificador con precio extra suma al total de la línea")
    void precioExtraSuma() {
        UUID it = item(negocioA, "PLATO-1");
        UUID g = grupo(negocioA, "Extras", 0, 3);
        UUID queso = opcion(negocioA, g, "Extra queso", "5000");
        UUID tocino = opcion(negocioA, g, "Extra tocino", "7000");
        vincular(negocioA, it, g);

        CotizacionDeModificadores cot = cotizar(negocioA, it, List.of(queso, tocino));
        assertThat(cot.extraTotal()).isEqualByComparingTo("12000");
        assertThat(cot.elegidos()).hasSize(2);

        assertThat(cotizar(negocioA, it, List.of()).extraTotal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Un modificador de un grupo que no aplica al ítem se rechaza")
    void modificadorAjenoAlItem() {
        UUID it = item(negocioA, "PLATO-1");
        UUID gVinculado = grupo(negocioA, "Extras", 0, 3);
        vincular(negocioA, it, gVinculado);
        UUID gSuelto = grupo(negocioA, "Otro", 0, 3);
        UUID suelta = opcion(negocioA, gSuelto, "No aplica", "1000");

        assertThatThrownBy(() -> cotizar(negocioA, it, List.of(suelta)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Vincular y desvincular un grupo del ítem")
    void vincularYDesvincular() {
        UUID it = item(negocioA, "PLATO-1");
        UUID g = grupo(negocioA, "Extras", 0, 3);
        opcion(negocioA, g, "Extra queso", "5000");

        vincular(negocioA, it, g);
        assertThat(enContexto(negocioA, admin, ADMIN, () -> modificadores.gruposDeItem(it)))
                .singleElement().satisfies(gc -> {
                    assertThat(gc.grupo().id()).isEqualTo(g);
                    assertThat(gc.opciones()).hasSize(1);
                });

        enContexto(negocioA, admin, ADMIN, () -> modificadores.desvincular(it, g));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> modificadores.gruposDeItem(it)))
                .isEmpty();
    }

    @Test
    @DisplayName("Eliminar un grupo se lleva sus opciones y sus vínculos")
    void eliminarGrupoCascada() {
        UUID it = item(negocioA, "PLATO-1");
        UUID g = grupo(negocioA, "Extras", 0, 3);
        opcion(negocioA, g, "Extra queso", "5000");
        vincular(negocioA, it, g);

        enContexto(negocioA, admin, ADMIN, () -> modificadores.eliminarGrupo(g));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> modificadores.gruposDeItem(it)))
                .isEmpty();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> modificadores.listarGrupos())).isEmpty();
    }

    @Test
    @DisplayName("El segundo negocio no ve los grupos del primero")
    void aislamiento() {
        UUID g = grupo(negocioA, "Extras", 0, 3);
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> modificadores.verGrupo(g)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
