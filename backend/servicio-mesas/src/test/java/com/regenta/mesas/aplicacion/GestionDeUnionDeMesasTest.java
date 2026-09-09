package com.regenta.mesas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.mesas.BaseDeMesas;

/** HU-083. Unir mesas para un grupo grande, con dos negocios cargados. */
class GestionDeUnionDeMesasTest extends BaseDeMesas {

    private static final Set<String> ADMIN = Set.of("MESAS_MESA_VER", "MESAS_MESA_CREAR",
            "MESAS_MESA_EDITAR");

    @Autowired
    private GestionDeSesionesDeMesa sesiones;
    @Autowired
    private GestionDeMesas mesas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();

    private UUID mesa(UUID negocio, String codigo) {
        return enContexto(negocio, mesero, ADMIN, () -> mesas.crear(new SolicitudDeMesa(null, codigo,
                null, 4, "CUADRADA", 0, 0, 80, 80))).id();
    }

    private UUID abrir(UUID negocio, UUID mesaId, int comensales) {
        return enContexto(negocio, mesero, ADMIN,
                () -> sesiones.abrir(mesaId, new SolicitudDeApertura(comensales))).id();
    }

    private String estadoMesa(UUID mesaId) {
        return consultar("SELECT estado FROM mesas WHERE id = '" + mesaId + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: dos mesas libres unidas comparten una sola sesión")
    void unir() {
        UUID a = mesa(negocioA, "M1");
        UUID b = mesa(negocioA, "M2");
        UUID s = abrir(negocioA, a, 4);

        SesionDelNegocio tras = enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, b));

        assertThat(tras.mesaIds()).containsExactly(a, b);
        assertThat(estadoMesa(b)).isEqualTo("OCUPADA");
        // La sesión que ve la mesa unida es la misma.
        assertThat(enContexto(negocioA, mesero, ADMIN, () -> sesiones.sesionActual(b)).id())
                .isEqualTo(s);
        assertThat(contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + s
                + "' AND tipo_evento = 'mesas_unidas'")).isEqualTo(1);
    }

    @Test
    @DisplayName("Unir la misma mesa dos veces no la agrega dos veces (idempotente)")
    void unirEsIdempotente() {
        UUID a = mesa(negocioA, "M1");
        UUID b = mesa(negocioA, "M2");
        UUID s = abrir(negocioA, a, 4);

        enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, b));
        SesionDelNegocio tras = enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, b));

        assertThat(tras.mesaIds()).containsExactly(a, b);
        assertThat(contar("SELECT count(*) FROM sesion_mesas WHERE sesion_id = '" + s + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 2: una mesa ya ocupada no se puede unir a otra sesión")
    void mesaOcupadaNoSeUne() {
        UUID a = mesa(negocioA, "M1");
        UUID b = mesa(negocioA, "M2");
        UUID sa = abrir(negocioA, a, 2);
        abrir(negocioA, b, 2); // b tiene su propia sesión

        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(sa, b)))
                .isInstanceOf(ConflictoDeEstadoException.class);

        // Y una mesa ya unida a otro grupo tampoco.
        UUID c = mesa(negocioA, "M3");
        UUID d = mesa(negocioA, "M4");
        UUID sc = abrir(negocioA, c, 2);
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(sc, d));
        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(sa, d)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 3: al cerrar la cuenta, todas las mesas del grupo pasan a SUCIA")
    void cerrarEnsuciaTodoElGrupo() {
        UUID a = mesa(negocioA, "M1");
        UUID b = mesa(negocioA, "M2");
        UUID c = mesa(negocioA, "M3");
        UUID s = abrir(negocioA, a, 8);
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, b));
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, c));

        enContexto(negocioA, mesero, ADMIN, () -> sesiones.cerrar(s));

        assertThat(estadoMesa(a)).isEqualTo("SUCIA");
        assertThat(estadoMesa(b)).isEqualTo("SUCIA");
        assertThat(estadoMesa(c)).isEqualTo("SUCIA");
    }

    @Test
    @DisplayName("Criterio 4: en el plano, las mesas unidas llevan el mismo sesionId")
    void planoMuestraLasUnidas() {
        UUID a = mesa(negocioA, "M1");
        UUID b = mesa(negocioA, "M2");
        UUID libre = mesa(negocioA, "M3");
        UUID s = abrir(negocioA, a, 4);
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, b));

        PlanoDelSalon plano = enContexto(negocioA, mesero, ADMIN, () -> mesas.plano());
        var porId = plano.sinZona().stream()
                .collect(java.util.stream.Collectors.toMap(MesaDelNegocio::id, m -> m));

        assertThat(porId.get(a).sesionId()).isEqualTo(s);
        assertThat(porId.get(b).sesionId()).isEqualTo(s);
        assertThat(porId.get(libre).sesionId()).isNull();
    }

    @Test
    @DisplayName("Separar una mesa del grupo la deja SUCIA y fuera de la sesión")
    void separar() {
        UUID a = mesa(negocioA, "M1");
        UUID b = mesa(negocioA, "M2");
        UUID s = abrir(negocioA, a, 4);
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.unir(s, b));

        SesionDelNegocio tras = enContexto(negocioA, mesero, ADMIN, () -> sesiones.separar(s, b));

        assertThat(tras.mesaIds()).containsExactly(a);
        assertThat(estadoMesa(b)).isEqualTo("SUCIA");
        // La principal no se separa.
        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN, () -> sesiones.separar(s, a)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no puede unir a una sesión del primero")
    void aislamiento() {
        UUID a = mesa(negocioA, "M1");
        UUID s = abrir(negocioA, a, 2);
        UUID bDeB = mesa(negocioB, "M9");

        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN, () -> sesiones.unir(s, bDeB)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
