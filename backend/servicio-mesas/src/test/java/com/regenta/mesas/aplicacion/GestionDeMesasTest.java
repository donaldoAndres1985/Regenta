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
import com.regenta.mesas.aplicacion.PlanoDelSalon.ZonaConMesas;

/** HU-081. Mesas y su posición en el plano, con dos negocios cargados. */
class GestionDeMesasTest extends BaseDeMesas {

    private static final Set<String> ADMIN = Set.of("MESAS_MESA_VER", "MESAS_MESA_CREAR",
            "MESAS_MESA_EDITAR");

    @Autowired
    private GestionDeMesas mesas;
    @Autowired
    private GestionDeZonas zonas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID zona(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN,
                () -> zonas.crear(new SolicitudDeZona(nombre, null, null))).id();
    }

    private MesaDelNegocio crear(UUID negocio, UUID zonaId, String codigo, Integer cap, int x, int y) {
        return enContexto(negocio, admin, ADMIN, () -> mesas.crear(new SolicitudDeMesa(zonaId, codigo,
                null, cap, "CUADRADA", x, y, 80, 80)));
    }

    @Test
    @DisplayName("Criterio 1: una mesa se crea con código, capacidad, zona y posición")
    void crearMesaCompleta() {
        UUID z = zona(negocioA, "Terraza");
        MesaDelNegocio m = crear(negocioA, z, "T1", 6, 140, 90);

        assertThat(m.zonaId()).isEqualTo(z);
        assertThat(m.codigo()).isEqualTo("T1");
        assertThat(m.capacidad()).isEqualTo(6);
        assertThat(m.posX()).isEqualTo(140);
        assertThat(m.posY()).isEqualTo(90);
        assertThat(m.estado()).isEqualTo("LIBRE");
    }

    @Test
    @DisplayName("Criterio 2: un código de mesa repetido en el negocio responde 409")
    void codigoRepetido() {
        crear(negocioA, null, "M1", 4, 0, 0);
        assertThatThrownBy(() -> crear(negocioA, null, "m1", 4, 10, 10))
                .isInstanceOf(RecursoDuplicadoException.class);
        // El mismo código en otro negocio sí se puede.
        assertThat(crear(negocioB, null, "M1", 4, 0, 0).id()).isNotNull();
    }

    @Test
    @DisplayName("Criterio 3: al mover una mesa su posición se guarda y se ve igual la próxima vez")
    void moverPersiste() {
        UUID id = crear(negocioA, null, "M1", 4, 0, 0).id();

        enContexto(negocioA, admin, ADMIN,
                () -> mesas.mover(id, new SolicitudDePosicion(320, 210, 100, 100)));

        MesaDelNegocio tras = enContexto(negocioA, admin, ADMIN, () -> mesas.ver(id));
        assertThat(tras.posX()).isEqualTo(320);
        assertThat(tras.posY()).isEqualTo(210);
        assertThat(tras.ancho()).isEqualTo(100);
        assertThat(consultar("SELECT pos_x FROM mesas WHERE id = '" + id + "'").get(0))
                .isEqualTo("320");
    }

    @Test
    @DisplayName("Criterio 4: una mesa con una sesión abierta no se puede eliminar (409)")
    void noSeBorraMesaOcupada() {
        UUID id = crear(negocioA, null, "M1", 4, 0, 0).id();
        ejecutarComoElServicio(negocioA, "INSERT INTO sesiones_mesa "
                + "(id, negocio_id, mesa_principal_id, estado) VALUES ('" + UUID.randomUUID()
                + "', '" + negocioA + "', '" + id + "', 'ABIERTA')");

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> mesas.eliminar(id)))
                .isInstanceOf(ConflictoDeEstadoException.class);
        // Sigue estando.
        assertThat(enContexto(negocioA, admin, ADMIN, () -> mesas.ver(id)).id()).isEqualTo(id);
    }

    @Test
    @DisplayName("Una mesa libre se elimina (soft) y desaparece del listado")
    void borrarMesaLibre() {
        UUID id = crear(negocioA, null, "M1", 4, 0, 0).id();
        enContexto(negocioA, admin, ADMIN, () -> mesas.eliminar(id));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> mesas.listar(null))).isEmpty();
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> mesas.ver(id)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El plano agrupa las mesas por zona, en el orden de las zonas")
    void plano() {
        UUID salon = enContexto(negocioA, admin, ADMIN,
                () -> zonas.crear(new SolicitudDeZona("Salón", 5, null))).id();
        UUID terraza = enContexto(negocioA, admin, ADMIN,
                () -> zonas.crear(new SolicitudDeZona("Terraza", 0, null))).id();
        crear(negocioA, salon, "S1", 4, 0, 0);
        crear(negocioA, terraza, "T1", 2, 0, 0);
        crear(negocioA, null, "BARRA", 1, 0, 0);

        PlanoDelSalon p = enContexto(negocioA, admin, ADMIN, () -> mesas.plano());
        assertThat(p.zonas()).extracting(zc -> zc.zona().nombre())
                .containsExactly("Terraza", "Salón"); // Terraza orden 0
        assertThat(p.zonas()).filteredOn(zc -> zc.zona().nombre().equals("Salón"))
                .flatExtracting(ZonaConMesas::mesas).extracting(MesaDelNegocio::codigo)
                .containsExactly("S1");
        assertThat(p.sinZona()).extracting(MesaDelNegocio::codigo).containsExactly("BARRA");
    }

    @Test
    @DisplayName("Una zona inexistente al crear la mesa responde 404")
    void zonaInexistente() {
        assertThatThrownBy(() -> crear(negocioA, UUID.randomUUID(), "M1", 4, 0, 0))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve ni toca las mesas del primero")
    void aislamiento() {
        UUID id = crear(negocioA, null, "M1", 4, 0, 0).id();
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> mesas.ver(id)))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN,
                () -> mesas.mover(id, new SolicitudDePosicion(1, 1, 1, 1))))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(enContexto(negocioB, admin, ADMIN, () -> mesas.listar(null))).isEmpty();
    }
}
