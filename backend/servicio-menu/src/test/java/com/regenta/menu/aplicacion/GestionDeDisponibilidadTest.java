package com.regenta.menu.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.menu.BaseDeMenu;

/** HU-080. Disponibilidad diaria de ítems, con dos negocios cargados. */
class GestionDeDisponibilidadTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private GestionDeDisponibilidad disponibilidad;
    @Autowired
    private GestionDeItemsDeMenu items;
    @Autowired
    private GestionDeCartas cartas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID jefe = UUID.randomUUID();

    private UUID carta(UUID negocio) {
        return enContexto(negocio, jefe, ADMIN, () -> cartas.crear(new SolicitudDeCarta(
                "Carta " + UUID.randomUUID(), null, null, null, null, null, null, false, null))).id();
    }

    private UUID itemEn(UUID negocio, UUID cartaId) {
        UUID cat = enContexto(negocio, jefe, ADMIN, () -> cartas.crearCategoria(cartaId,
                new SolicitudDeCategoria("Fuertes", null, 0, null))).id();
        return enContexto(negocio, jefe, ADMIN, () -> items.crear(new SolicitudDeItem(cat, null,
                "IT-" + UUID.randomUUID().toString().substring(0, 8), "Bandeja", null, "PLATO",
                new BigDecimal("30000"), null, true, null, "FUERTE", null, null, 0))).id();
    }

    private ItemEnCarta enLaCarta(UUID negocio, UUID cartaId, UUID itemId) {
        return enContexto(negocio, jefe, ADMIN, () -> items.menuDeCarta(cartaId)).categorias()
                .stream().flatMap(cc -> cc.items().stream())
                .filter(it -> it.id().equals(itemId)).findFirst().orElseThrow();
    }

    private long eventos(UUID itemId, String tipo) {
        return contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + itemId
                + "' AND tipo_evento = '" + tipo + "'");
    }

    @Test
    @DisplayName("Criterio 1: un ítem marcado agotado aparece agotado y no pedible en la carta del mesero")
    void marcarAgotado() {
        UUID c = carta(negocioA);
        UUID it = itemEn(negocioA, c);
        assertThat(enLaCarta(negocioA, c, it).pedible()).isTrue();

        enContexto(negocioA, jefe, ADMIN, () -> disponibilidad.marcarAgotado(it));

        ItemEnCarta agotado = enLaCarta(negocioA, c, it);
        assertThat(agotado.disponible()).isFalse();
        assertThat(agotado.pedible()).isFalse();
        assertThat(eventos(it, "item_agotado")).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 1: reponer el ítem lo devuelve a la carta")
    void reponer() {
        UUID c = carta(negocioA);
        UUID it = itemEn(negocioA, c);
        enContexto(negocioA, jefe, ADMIN, () -> disponibilidad.marcarAgotado(it));
        enContexto(negocioA, jefe, ADMIN, () -> disponibilidad.reponer(it));

        assertThat(enLaCarta(negocioA, c, it).pedible()).isTrue();
        assertThat(eventos(it, "item_disponible")).isEqualTo(1);
    }

    @Test
    @DisplayName("Marcar dos veces agotado no publica el evento dos veces")
    void marcarAgotadoEsIdempotente() {
        UUID it = itemEn(negocioA, carta(negocioA));
        enContexto(negocioA, jefe, ADMIN, () -> disponibilidad.marcarAgotado(it));
        enContexto(negocioA, jefe, ADMIN, () -> disponibilidad.marcarAgotado(it));
        assertThat(eventos(it, "item_agotado")).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 2: la disponibilidad de ayer no arrastra a hoy")
    void seReiniciaAlCambiarDeDia() {
        UUID c = carta(negocioA);
        UUID it = itemEn(negocioA, c);

        enContexto(negocioA, jefe, ADMIN,
                () -> disponibilidad.marcarAgotado(it, LocalDate.now().minusDays(1)));

        assertThat(enLaCarta(negocioA, c, it).pedible()).as("hoy vuelve a estar pedible").isTrue();
        assertThat(enContexto(negocioA, jefe, ADMIN,
                () -> disponibilidad.delDia(it, LocalDate.now())).agotado()).isFalse();
        assertThat(enContexto(negocioA, jefe, ADMIN,
                () -> disponibilidad.delDia(it, LocalDate.now().minusDays(1))).agotado()).isTrue();
    }

    @Test
    @DisplayName("Criterio 4: al llegar al cupo diario el ítem se marca agotado solo")
    void cupoDiario() {
        UUID c = carta(negocioA);
        UUID it = itemEn(negocioA, c);
        enContexto(negocioA, jefe, ADMIN, () -> disponibilidad.fijarCupo(it, 3));

        enContexto(negocioA, jefe, ADMIN,
                () -> disponibilidad.registrarVentas(negocioA, Map.of(it, 2), LocalDate.now()));
        assertThat(enLaCarta(negocioA, c, it).pedible()).isTrue();

        enContexto(negocioA, jefe, ADMIN,
                () -> disponibilidad.registrarVentas(negocioA, Map.of(it, 1), LocalDate.now()));
        assertThat(enLaCarta(negocioA, c, it).pedible()).isFalse();
        assertThat(eventos(it, "item_agotado")).isEqualTo(1);
    }

    @Test
    @DisplayName("delDia devuelve disponible cuando no hay fila para esa fecha")
    void delDiaSinFila() {
        UUID it = itemEn(negocioA, carta(negocioA));
        DisponibilidadDelItem estado = enContexto(negocioA, jefe, ADMIN,
                () -> disponibilidad.delDia(it, LocalDate.now()));
        assertThat(estado.agotado()).isFalse();
        assertThat(estado.cupoDiario()).isNull();
        assertThat(estado.vendidas()).isZero();
    }

    @Test
    @DisplayName("El segundo negocio no puede tocar ni ver la disponibilidad del primero")
    void aislamiento() {
        UUID it = itemEn(negocioA, carta(negocioA));
        assertThatThrownBy(() -> enContexto(negocioB, jefe, ADMIN,
                () -> disponibilidad.marcarAgotado(it)))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> enContexto(negocioB, jefe, ADMIN,
                () -> disponibilidad.delDia(it, LocalDate.now())))
                .isInstanceOf(NoEncontradoException.class);
    }
}
