package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.domain.AtributoCategoria;
import com.regenta.inventario.domain.TipoAtributo;

/** HU-028: la validacion del JSONB contra atributos_categoria, sin Spring. */
class ValidadorDeAtributosTest {

    private final ValidadorDeAtributos validador = new ValidadorDeAtributos();
    private final UUID negocio = UUID.randomUUID();
    private final UUID categoria = UUID.randomUUID();

    private AtributoCategoria atributo(String campo, TipoAtributo tipo, boolean obligatorio) {
        AtributoCategoria a = AtributoCategoria.nuevo(negocio, categoria, campo, campo, tipo);
        a.configurar(obligatorio, null, tipo.esDeOpciones() ? List.of("S", "M", "L") : null,
                null, null, null, null, null, null);
        return a;
    }

    @Test
    void faltanObligatorios_nombraLosCampos() {
        var defs = List.of(
                atributo("lote", TipoAtributo.TEXTO, true),
                atributo("fecha_vencimiento", TipoAtributo.FECHA, true));

        assertThatThrownBy(() -> validador.validar(defs, Map.of("otro", "x")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("lote")
                .hasMessageContaining("fecha_vencimiento");
    }

    @Test
    void conTodoEnRegla_devuelveElMapa() {
        var defs = List.of(
                atributo("lote", TipoAtributo.TEXTO, true),
                atributo("fecha_vencimiento", TipoAtributo.FECHA, true));

        Map<String, Object> ok = validador.validar(defs,
                Map.of("lote", "L-2026-01", "fecha_vencimiento", "2027-03-01"));

        assertThat(ok).containsEntry("lote", "L-2026-01");
    }

    @Test
    void numeroConTexto_falla() {
        var defs = List.of(atributo("calibre", TipoAtributo.NUMERO, true));

        assertThatThrownBy(() -> validador.validar(defs, Map.of("calibre", "gordo")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("calibre");
    }

    @Test
    void numeroConDecimales_falla() {
        var defs = List.of(atributo("piezas", TipoAtributo.NUMERO, true));

        assertThatThrownBy(() -> validador.validar(defs, Map.of("piezas", "2.5")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void decimalAceptaComaDecimalNo_puntoSi() {
        var defs = List.of(atributo("peso", TipoAtributo.DECIMAL, true));

        assertThat(validador.validar(defs, Map.of("peso", "1.75"))).containsKey("peso");
        assertThatThrownBy(() -> validador.validar(defs, Map.of("peso", "1,75")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void listaFueraDeOpciones_falla() {
        var defs = List.of(atributo("talla", TipoAtributo.LISTA, true));

        assertThatThrownBy(() -> validador.validar(defs, Map.of("talla", "XXL")))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(validador.validar(defs, Map.of("talla", "M"))).containsEntry("talla", "M");
    }

    @Test
    void rangoNumerico_seRespeta() {
        AtributoCategoria calibre = AtributoCategoria.nuevo(negocio, categoria, "calibre", "Calibre",
                TipoAtributo.NUMERO);
        calibre.configurar(true, null, null, null, null, new BigDecimal("1"), new BigDecimal("10"),
                null, null);

        assertThatThrownBy(() -> validador.validar(List.of(calibre), Map.of("calibre", "20")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("mayor que 10");
        assertThat(validador.validar(List.of(calibre), Map.of("calibre", "5")))
                .containsEntry("calibre", "5");
    }

    @Test
    void opcionalAusente_noEsError() {
        var defs = List.of(atributo("nota", TipoAtributo.TEXTO, false));

        assertThat(validador.validar(defs, Map.of())).isEmpty();
    }
}
