package com.regenta.menu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-078. Reglas puras de un grupo de modificadores: los mínimos y máximos. */
class GrupoDeModificadoresTest {

    private static final UUID NEG = UUID.randomUUID();

    @Test
    @DisplayName("Un grupo con máximo menor que el mínimo se rechaza")
    void maximoMenorQueMinimo() {
        assertThatThrownBy(() -> GrupoDeModificadores.crear(NEG, "Extras", 3, 1))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> GrupoDeModificadores.crear(NEG, "Extras", -1, 2))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> GrupoDeModificadores.crear(NEG, "Extras", 0, 0))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatCode(() -> GrupoDeModificadores.crear(NEG, "Extras", 1, 3))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("permiteSeleccion respeta el mínimo y el máximo")
    void permiteSeleccion() {
        GrupoDeModificadores g = GrupoDeModificadores.crear(NEG, "Término", 1, 5);

        assertThat(g.permiteSeleccion(0)).isFalse();
        assertThat(g.permiteSeleccion(1)).isTrue();
        assertThat(g.permiteSeleccion(5)).isTrue();
        assertThat(g.permiteSeleccion(6)).isFalse();
    }

    @Test
    @DisplayName("Un grupo con mínimo mayor que cero es obligatorio")
    void obligatorio() {
        assertThat(GrupoDeModificadores.crear(NEG, "Término", 1, 1).esObligatorio()).isTrue();
        assertThat(GrupoDeModificadores.crear(NEG, "Extras", 0, 3).esObligatorio()).isFalse();
    }
}
