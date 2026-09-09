package com.regenta.menu.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.menu.BaseDeMenu;

/** HU-077. Estaciones de cocina, con dos negocios cargados. */
class GestionDeEstacionesDeCocinaTest extends BaseDeMenu {

    private static final Set<String> ADMIN = Set.of("MENU_PLATO_VER", "MENU_PLATO_CREAR",
            "MENU_PLATO_EDITAR");

    @Autowired
    private GestionDeEstacionesDeCocina estaciones;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private EstacionDelNegocio crear(UUID negocio, String codigo) {
        return enContexto(negocio, admin, ADMIN, () -> estaciones.crear(
                new SolicitudDeEstacion(codigo, "Estación " + codigo, "impresora-1", 1, null)));
    }

    @Test
    @DisplayName("El código de la estación se normaliza y es único por negocio")
    void codigoUnico() {
        EstacionDelNegocio parrilla = crear(negocioA, "parrilla");
        assertThat(parrilla.codigo()).isEqualTo("PARRILLA");

        assertThatThrownBy(() -> crear(negocioA, "PARRILLA"))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThat(crear(negocioB, "PARRILLA").id()).isNotNull();
    }

    @Test
    @DisplayName("Se activa y se desactiva; el listado va por orden")
    void activacionYListado() {
        UUID id = crear(negocioA, "FRIA").id();
        crear(negocioA, "BAR");

        enContexto(negocioA, admin, ADMIN, () -> estaciones.cambiarActivacion(id, false));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> estaciones.ver(id)).activa()).isFalse();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> estaciones.listar())).hasSize(2);
    }

    @Test
    @DisplayName("El segundo negocio no ve las estaciones del primero")
    void aislamiento() {
        UUID enA = crear(negocioA, "PARRILLA").id();
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> estaciones.ver(enA)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
