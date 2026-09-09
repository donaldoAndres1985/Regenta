package com.regenta.reservas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-072 criterio 4. El titular de la estancia queda identificado con su documento. */
class OcupanteTest {

    private static final UUID NEG = UUID.randomUUID();
    private static final UUID RES = UUID.randomUUID();

    @Test
    @DisplayName("El titular sin documento se rechaza")
    void titularSinDocumento() {
        assertThatThrownBy(() -> Ocupante.nuevo(NEG, RES, true, "Ada", "Lovelace", null, null, null,
                null, null, null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Ocupante.nuevo(NEG, RES, true, "Ada", "Lovelace", "CC", "  ", null,
                null, null, null)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El titular con documento queda identificado; un acompañante puede ir sin documento")
    void titularConDocumento() {
        Ocupante titular = Ocupante.nuevo(NEG, RES, true, "  Ada ", "Lovelace", "CC", "51234567",
                "co", null, "3001112233", null);
        assertThat(titular.esTitular()).isTrue();
        assertThat(titular.getNombres()).isEqualTo("Ada");
        assertThat(titular.getNumeroDocumento()).isEqualTo("51234567");
        assertThat(titular.getNacionalidad()).isEqualTo("CO");

        assertThatCode(() -> Ocupante.nuevo(NEG, RES, false, "Grace", null, null, null, null, null,
                null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un nombre en blanco o una nacionalidad que no sea de dos letras se rechazan")
    void validacionesDeCampos() {
        assertThatThrownBy(() -> Ocupante.nuevo(NEG, RES, false, "  ", null, null, null, null, null,
                null, null)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> Ocupante.nuevo(NEG, RES, false, "Grace", null, null, null, "COL",
                null, null, null)).isInstanceOf(ReglaDeNegocioException.class);
    }
}
