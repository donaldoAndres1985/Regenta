package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.recursos.domain.AtributoTipoRecurso;
import com.regenta.recursos.domain.TipoAtributoRecurso;

/** HU-064 criterio 4: el validador de atributos, sin Spring. */
class ValidadorDeAtributosDeRecursoTest {

    private final ValidadorDeAtributosDeRecurso validador = new ValidadorDeAtributosDeRecurso();

    private AtributoTipoRecurso atributo(String campo, TipoAtributoRecurso tipo, boolean obligatorio,
            List<String> opciones) {
        return AtributoTipoRecurso.nuevo(UUID.randomUUID(), UUID.randomUUID(), campo, campo, tipo,
                obligatorio, opciones, 0);
    }

    @Test
    @DisplayName("Un atributo obligatorio ausente hace fallar la validación (422)")
    void obligatorioAusente() {
        var defs = List.of(atributo("vista", TipoAtributoRecurso.TEXTO, true, null));

        assertThatThrownBy(() -> validador.validar(defs, Map.of()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("vista");
    }

    @Test
    @DisplayName("Con el obligatorio presente y del tipo correcto, pasa")
    void obligatorioPresente() {
        var defs = List.of(
                atributo("pisos", TipoAtributoRecurso.NUMERO, true, null),
                atributo("superficie", TipoAtributoRecurso.LISTA, false,
                        List.of("sintética", "natural")));

        assertThatCode(() -> validador.validar(defs,
                Map.of("pisos", 3, "superficie", "sintética"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un número que no es número falla; una opción fuera de lista falla")
    void tipoNoCuadra() {
        var num = List.of(atributo("pisos", TipoAtributoRecurso.NUMERO, false, null));
        assertThatThrownBy(() -> validador.validar(num, Map.of("pisos", "planta baja")))
                .isInstanceOf(ReglaDeNegocioException.class);

        var lista = List.of(atributo("superficie", TipoAtributoRecurso.LISTA, false,
                List.of("sintética", "natural")));
        assertThatThrownBy(() -> validador.validar(lista, Map.of("superficie", "cemento")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Un atributo opcional ausente no molesta")
    void opcionalAusente() {
        var defs = List.of(atributo("nota", TipoAtributoRecurso.TEXTO, false, null));
        assertThatCode(() -> validador.validar(defs, Map.of())).doesNotThrowAnyException();
    }
}
