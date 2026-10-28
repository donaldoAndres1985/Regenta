package com.regenta.comun.negocio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * HU-138. Con qué identidad llama un servicio a otro. Si hay un usuario detrás,
 * se reenvía la suya: el otro servicio aplica sus permisos. Si no lo hay (un
 * barrido, un listener), va como SISTEMA con solo el permiso de lectura que
 * necesita esa consulta (decisión del 27-oct en #258).
 */
class IdentidadSalienteTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID sucursal = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private HttpHeaders cabecerasCon(DatosDelNegocio quien) {
        HttpHeaders cabeceras = new HttpHeaders();
        ContextoDeNegocio.en(quien, () -> IdentidadSaliente.aplicar(cabeceras, negocio,
                "INVENTARIO", "INVENTARIO_PRODUCTO_VER"));
        return cabeceras;
    }

    @Test
    @DisplayName("Con un usuario detrás, se reenvía su identidad tal cual")
    void reenviaLaDelUsuario() {
        HttpHeaders cabeceras = cabecerasCon(new DatosDelNegocio(negocio, usuario, "PROFESIONAL",
                "COMANDA", Set.of("ADMINISTRADOR"), Set.of("MENU", "INVENTARIO"),
                Set.of("MENU_PLATO_EDITAR", "INVENTARIO_PRODUCTO_VER"), Set.of(sucursal)));

        assertThat(cabeceras.getFirst(CabecerasDeNegocio.NEGOCIO)).isEqualTo(negocio.toString());
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.USUARIO)).isEqualTo(usuario.toString());
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.PLAN)).isEqualTo("PROFESIONAL");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.PATRON)).isEqualTo("COMANDA");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.ROLES)).isEqualTo("ADMINISTRADOR");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.MODULOS).split(","))
                .containsExactlyInAnyOrder("MENU", "INVENTARIO");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.PERMISOS).split(","))
                .containsExactlyInAnyOrder("MENU_PLATO_EDITAR", "INVENTARIO_PRODUCTO_VER");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.SUCURSALES))
                .isEqualTo(sucursal.toString());
    }

    @Test
    @DisplayName("Sin usuario, va como SISTEMA con el módulo de destino y solo el permiso de esa consulta")
    void sinUsuarioVaComoSistema() {
        HttpHeaders cabeceras = cabecerasCon(new DatosDelNegocio(negocio, null, "", "COMANDA",
                Set.of(), Set.of("MENU"), Set.of(), Set.of()));

        assertThat(cabeceras.getFirst(CabecerasDeNegocio.NEGOCIO)).isEqualTo(negocio.toString());
        assertThat(cabeceras.containsKey(CabecerasDeNegocio.USUARIO)).isFalse();
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.PATRON)).isEqualTo("COMANDA");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.ROLES)).isEqualTo("SISTEMA");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.MODULOS)).isEqualTo("INVENTARIO");
        assertThat(cabeceras.getFirst(CabecerasDeNegocio.PERMISOS))
                .isEqualTo("INVENTARIO_PRODUCTO_VER");
    }

    @Test
    @DisplayName("El negocio de la cabecera es el de la consulta, no otro que haya quedado en el contexto")
    void elNegocioEsElDeLaConsulta() {
        UUID otro = UUID.randomUUID();
        HttpHeaders cabeceras = new HttpHeaders();
        ContextoDeNegocio.en(new DatosDelNegocio(otro, null, "", "", Set.of("SISTEMA"), Set.of(),
                Set.of(), Set.of()), () -> IdentidadSaliente.aplicar(cabeceras, negocio,
                        "USUARIOS", "USUARIOS_USUARIO_VER"));

        assertThat(cabeceras.getFirst(CabecerasDeNegocio.NEGOCIO)).isEqualTo(negocio.toString());
    }
}
