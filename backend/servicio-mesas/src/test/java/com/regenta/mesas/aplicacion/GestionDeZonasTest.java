package com.regenta.mesas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.mesas.BaseDeMesas;

/** HU-081. Zonas del salón, con dos negocios cargados. */
class GestionDeZonasTest extends BaseDeMesas {

    private static final Set<String> ADMIN = Set.of("MESAS_MESA_VER", "MESAS_MESA_CREAR",
            "MESAS_MESA_EDITAR");

    @Autowired
    private GestionDeZonas zonas;
    @Autowired
    private GestionDeMesas mesas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private ZonaDelNegocio crear(UUID negocio, String nombre, Integer orden, String color) {
        return enContexto(negocio, admin, ADMIN,
                () -> zonas.crear(new SolicitudDeZona(nombre, orden, color)));
    }

    @Test
    @DisplayName("Una zona se crea con nombre, color y orden")
    void crearZona() {
        ZonaDelNegocio z = crear(negocioA, "VIP", 3, "#1E88E5");
        assertThat(z.nombre()).isEqualTo("VIP");
        assertThat(z.orden()).isEqualTo(3);
        assertThat(z.color()).isEqualTo("#1E88E5");
        assertThat(z.activa()).isTrue();
    }

    @Test
    @DisplayName("Un nombre de zona repetido en el negocio responde 409")
    void nombreRepetido() {
        crear(negocioA, "Salón", null, null);
        assertThatThrownBy(() -> crear(negocioA, "salón", null, null))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThat(crear(negocioB, "Salón", null, null).id()).isNotNull();
    }

    @Test
    @DisplayName("Sin orden explícito, la zona nueva va al final")
    void ordenAutomatico() {
        crear(negocioA, "Salón", null, null);
        crear(negocioA, "Terraza", null, null);
        assertThat(enContexto(negocioA, admin, ADMIN, () -> zonas.listar()))
                .extracting(ZonaDelNegocio::orden).containsExactly(0, 1);
    }

    @Test
    @DisplayName("Reordenar fija el orden según la lista recibida")
    void reordenar() {
        UUID a = crear(negocioA, "Salón", null, null).id();
        UUID b = crear(negocioA, "Terraza", null, null).id();
        UUID c = crear(negocioA, "Barra", null, null).id();

        enContexto(negocioA, admin, ADMIN, () -> zonas.reordenar(List.of(c, a, b)));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> zonas.listar()))
                .extracting(ZonaDelNegocio::nombre).containsExactly("Barra", "Salón", "Terraza");
    }

    @Test
    @DisplayName("Una zona con mesas no se puede borrar (409); vacía sí")
    void borrarZona() {
        UUID z = crear(negocioA, "Salón", null, null).id();
        enContexto(negocioA, admin, ADMIN, () -> mesas.crear(new SolicitudDeMesa(z, "S1", null, 4,
                "CUADRADA", 0, 0, 80, 80)));

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> zonas.eliminar(z)))
                .isInstanceOf(ConflictoDeEstadoException.class);

        UUID vacia = crear(negocioA, "Terraza", null, null).id();
        enContexto(negocioA, admin, ADMIN, () -> zonas.eliminar(vacia));
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> zonas.ver(vacia)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve las zonas del primero")
    void aislamiento() {
        UUID z = crear(negocioA, "Salón", null, null).id();
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> zonas.ver(z)))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(enContexto(negocioB, admin, ADMIN, () -> zonas.listar())).isEmpty();
    }
}
