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
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.mesas.BaseDeMesas;

/** HU-082. Sesión de mesa: abrir, ocupar y liberar, con dos negocios cargados. */
class GestionDeSesionesDeMesaTest extends BaseDeMesas {

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

    private String estadoMesa(UUID mesaId) {
        return consultar("SELECT estado FROM mesas WHERE id = '" + mesaId + "'").get(0);
    }

    private long eventos(UUID agregadoId, String tipo) {
        return contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + agregadoId
                + "' AND tipo_evento = '" + tipo + "'");
    }

    @Test
    @DisplayName("Criterio 1: abrir una mesa libre la pasa a OCUPADA y arranca el cronómetro")
    void abrir() {
        UUID m = mesa(negocioA, "M1");

        SesionDelNegocio s = enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(3)));

        assertThat(s.estado()).isEqualTo("ABIERTA");
        assertThat(s.numComensales()).isEqualTo(3);
        assertThat(s.minutosAbierta()).isGreaterThanOrEqualTo(0);
        assertThat(estadoMesa(m)).isEqualTo("OCUPADA");
        assertThat(eventos(s.id(), "sesion_mesa_abierta")).isEqualTo(1);
        assertThat(eventos(m, "mesa_ocupada")).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 2: no se puede abrir una segunda sesión sobre la misma mesa")
    void unaSolaSesionViva() {
        UUID m = mesa(negocioA, "M1");
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.abrir(m, new SolicitudDeApertura(2)));

        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(4))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 2 (backstop): uq_sesion_abierta rechaza dos sesiones vivas en la base")
    void backstopEnLaBase() {
        UUID m = mesa(negocioA, "M1");
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.abrir(m, new SolicitudDeApertura(2)));

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA, "INSERT INTO sesiones_mesa "
                + "(id, negocio_id, mesa_principal_id, num_comensales, estado) VALUES ('"
                + UUID.randomUUID() + "', '" + negocioA + "', '" + m + "', 2, 'ABIERTA')"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 3: cerrar la sesión deja la mesa SUCIA, no libre")
    void cerrarDejaSucia() {
        UUID m = mesa(negocioA, "M1");
        UUID s = enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(2))).id();

        enContexto(negocioA, mesero, ADMIN, () -> sesiones.cerrar(s));

        assertThat(estadoMesa(m)).isEqualTo("SUCIA");
        assertThat(enContexto(negocioA, mesero, ADMIN, () -> sesiones.ver(s)).estado())
                .isEqualTo("CERRADA");
        assertThat(eventos(s, "sesion_mesa_cerrada")).isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 4: marcar limpia pasa la mesa de SUCIA a LIBRE; desde otro estado se rechaza")
    void marcarLimpia() {
        UUID m = mesa(negocioA, "M1");
        UUID s = enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(2))).id();

        // Ocupada: todavía no se puede marcar limpia.
        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN, () -> sesiones.marcarLimpia(m)))
                .isInstanceOf(ReglaDeNegocioException.class);

        enContexto(negocioA, mesero, ADMIN, () -> sesiones.cerrar(s));
        enContexto(negocioA, mesero, ADMIN, () -> sesiones.marcarLimpia(m));

        assertThat(estadoMesa(m)).isEqualTo("LIBRE");
        assertThat(eventos(m, "mesa_liberada")).isEqualTo(1);
        // Y ahora sí se puede volver a abrir.
        assertThat(enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(5))).estado()).isEqualTo("ABIERTA");
    }

    @Test
    @DisplayName("Criterio 5: una sesión cerrada sabe cuánto duró y cuántos comensales tuvo")
    void resumenDeSesionCerrada() {
        UUID m = mesa(negocioA, "M1");
        UUID s = enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(6))).id();
        // Simula 90 minutos de servicio antes de cerrar.
        ejecutarComoElServicio(negocioA, "UPDATE sesiones_mesa "
                + "SET abierta_en = now() - interval '90 minutes' WHERE id = '" + s + "'");

        enContexto(negocioA, mesero, ADMIN, () -> sesiones.cerrar(s));

        SesionDelNegocio cerrada = enContexto(negocioA, mesero, ADMIN, () -> sesiones.ver(s));
        assertThat(cerrada.numComensales()).isEqualTo(6);
        assertThat(cerrada.duracionMin()).isEqualTo(90);
        assertThat(cerrada.cerradaEn()).isNotNull();
    }

    @Test
    @DisplayName("La sesión actual de una mesa libre responde 404")
    void sesionActualDeMesaLibre() {
        UUID m = mesa(negocioA, "M1");
        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN, () -> sesiones.sesionActual(m)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve ni toca las sesiones del primero")
    void aislamiento() {
        UUID m = mesa(negocioA, "M1");
        UUID s = enContexto(negocioA, mesero, ADMIN,
                () -> sesiones.abrir(m, new SolicitudDeApertura(2))).id();

        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN, () -> sesiones.ver(s)))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN, () -> sesiones.cerrar(s)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
