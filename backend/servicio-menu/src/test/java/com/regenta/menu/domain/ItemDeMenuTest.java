package com.regenta.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-077. Reglas puras de un ítem de menú: valores por defecto y si se puede pedir. */
class ItemDeMenuTest {

    private static final UUID NEG = UUID.randomUUID();
    private static final UUID CAT = UUID.randomUUID();

    private static ItemDeMenu item(BigDecimal precio, Short tiempo, Map<String, Object> atributos) {
        return ItemDeMenu.crear(NEG, CAT, UUID.randomUUID(), "PLATO-1", "Bandeja paisa", null,
                TipoDeItem.PLATO, precio, null, true, tiempo, CursoDeMenu.FUERTE, atributos, null, 0);
    }

    @Test
    @DisplayName("Un ítem nuevo nace disponible, activo y con costo estimado en cero")
    void valoresPorDefecto() {
        ItemDeMenu i = item(new BigDecimal("32000"), (short) 18, null);

        assertThat(i.isDisponible()).isTrue();
        assertThat(i.isActivo()).isTrue();
        assertThat(i.getCostoEstimado()).isEqualByComparingTo("0");
        assertThat(i.getAtributos()).isEmpty();
        assertThat(i.pedible()).isTrue();
    }

    @Test
    @DisplayName("Criterio 4: un ítem no disponible no se puede pedir, pero sigue siendo un ítem")
    void noDisponibleNoEsPedible() {
        ItemDeMenu i = item(new BigDecimal("10000"), null, null);
        i.marcarDisponible(false);

        assertThat(i.isDisponible()).isFalse();
        assertThat(i.pedible()).isFalse();
        assertThat(i.estaEliminado()).isFalse();
    }

    @Test
    @DisplayName("Un ítem eliminado queda inactivo y no pedible")
    void eliminadoNoEsPedible() {
        ItemDeMenu i = item(new BigDecimal("10000"), null, null);
        i.eliminar(OffsetDateTime.parse("2026-05-04T12:00:00Z"));

        assertThat(i.estaEliminado()).isTrue();
        assertThat(i.isActivo()).isFalse();
        assertThat(i.pedible()).isFalse();
    }

    @Test
    @DisplayName("Criterio 3: los atributos que se pasan quedan guardados")
    void atributosSeGuardan() {
        Map<String, Object> attrs = Map.of("alergenos", List.of("gluten", "lacteos"),
                "vegano", false, "picante", 2);
        ItemDeMenu i = item(new BigDecimal("10000"), null, attrs);

        assertThat(i.getAtributos()).containsEntry("vegano", false).containsEntry("picante", 2);
        assertThat(i.getAtributos().get("alergenos").toString()).contains("gluten", "lacteos");
    }

    @Test
    @DisplayName("Precio negativo se guarda como cero; tiempo negativo, como nulo")
    void saneamiento() {
        ItemDeMenu i = item(new BigDecimal("-5"), (short) -3, null);
        assertThat(i.getPrecio()).isEqualByComparingTo("0");
        assertThat(i.getTiempoPreparacionMin()).isNull();
    }

    @Test
    @DisplayName("Un tipo o un curso desconocido se rechazan")
    void enumsInvalidos() {
        assertThatThrownBy(() -> TipoDeItem.desde("SOPA"))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> CursoDeMenu.desde("APERITIVO"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
