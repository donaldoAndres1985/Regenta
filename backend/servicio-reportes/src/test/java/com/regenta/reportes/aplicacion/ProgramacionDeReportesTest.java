package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.reportes.BaseDeReportes;
import com.regenta.reportes.infra.PasarelaDeCorreoStub;

/**
 * HU-100 criterio 2. El contador no quiere entrar cada lunes a sacar lo
 * mismo: lo programa una vez y le llega.
 */
class ProgramacionDeReportesTest extends BaseDeReportes {

    private static final Set<String> CONTADOR =
            Set.of("REPORTES_REPORTE_VER", "REPORTES_REPORTE_EXPORTAR");

    /** Lunes a las 6 de la mañana. */
    private static final String CADA_LUNES = "0 0 6 * * MON";

    @Autowired
    private GestionDeProgramaciones programaciones;
    @Autowired
    private GestionDeExportaciones exportaciones;
    @Autowired
    private PasarelaDeCorreoStub correo;

    private final UUID negocio = UUID.randomUUID();
    private final UUID otroNegocio = UUID.randomUUID();
    private final UUID contador = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        correo.reiniciar();
        for (UUID quien : List.of(negocio, otroNegocio)) {
            ejecutarComoElServicio(quien, "insert into config_negocio (negocio_id, zona_horaria) "
                    + "values ('" + quien + "', 'America/Bogota') on conflict do nothing");
        }
    }

    private ReporteProgramado programar(UUID deQuien, String nombre) {
        return enContexto(deQuien, contador, CONTADOR, () -> programaciones.programar(
                new SolicitudDeProgramacion("VENTAS_POR_CATEGORIA", nombre, CADA_LUNES, "CSV",
                        List.of("contador@negocio.co"), 7)));
    }

    @Test
    @DisplayName("Criterio 2: al programar queda fijada la próxima ejecución, sacada del cron")
    void programarFijaLaProximaEjecucion() {
        ReporteProgramado semanal = programar(negocio, "Ventas de la semana");

        assertThat(semanal.proximaEjecucion())
                .as("la próxima cae en el futuro, no hoy a las 6 si ya pasaron las 6")
                .isAfter(java.time.OffsetDateTime.now());
        assertThat(semanal.proximaEjecucion().getDayOfWeek())
                .isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(semanal.destinatarios()).containsExactly("contador@negocio.co");
    }

    @Test
    @DisplayName("Un cron que no se entiende se rechaza al programarlo")
    void cronInvalido() {
        assertThatThrownBy(() -> enContexto(negocio, contador, CONTADOR,
                () -> programaciones.programar(new SolicitudDeProgramacion("VENTAS_POR_CATEGORIA",
                        "Malo", "todos los lunes", "CSV", List.of("a@b.co"), 7))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 2: cuando llega el momento se ejecuta y se envía a los destinatarios")
    void alLlegarElMomentoSeEjecutaYSeEnvia() {
        ReporteProgramado semanal = programar(negocio, "Ventas de la semana");
        // Llegó el lunes.
        ejecutarComoElServicio(negocio, "update reportes_programados set proxima_ejecucion = now() "
                + "where id = '" + semanal.id() + "'");

        int encoladas = enContexto(negocio, contador, CONTADOR, () -> programaciones.barrer());
        enContexto(negocio, contador, CONTADOR, () -> exportaciones.procesarPendientes());

        assertThat(encoladas).isEqualTo(1);
        assertThat(correo.enviados())
                .as("el reporte llega solo: de eso se trataba programarlo")
                .hasSize(1);
        assertThat(correo.enviados().get(0).destino()).isEqualTo("contador@negocio.co");
        assertThat(correo.enviados().get(0).adjunto().nombre()).endsWith(".csv");
        assertThat(comoElServicio(negocio, "select estado from ejecuciones_reporte "
                + "where programado_id = '" + semanal.id() + "'")).containsExactly("COMPLETADO");
    }

    @Test
    @DisplayName("Criterio 2: tras ejecutarse, la próxima queda agendada para la semana siguiente")
    void trasEjecutarseSeVuelveAAgendar() {
        ReporteProgramado semanal = programar(negocio, "Ventas de la semana");
        ejecutarComoElServicio(negocio, "update reportes_programados set proxima_ejecucion = now() "
                + "where id = '" + semanal.id() + "'");

        enContexto(negocio, contador, CONTADOR, () -> programaciones.barrer());

        assertThat(comoElServicio(negocio, "select proxima_ejecucion > now() "
                + "from reportes_programados where id = '" + semanal.id() + "'"))
                .as("un programado que no se reagenda se ejecuta una sola vez o en bucle")
                .containsExactly("t");
        assertThat(enContexto(negocio, contador, CONTADOR,
                () -> programaciones.ver(semanal.id())).proximaEjecucion().getDayOfWeek())
                .as("y la próxima sigue cayendo el día que pidió el cron")
                .isEqualTo(java.time.DayOfWeek.MONDAY);
        assertThat(enContexto(negocio, contador, CONTADOR, () -> programaciones.barrer()))
                .as("el mismo barrido dos veces no lo ejecuta dos veces")
                .isZero();
    }

    @Test
    @DisplayName("Una programación desactivada no se ejecuta")
    void laDesactivadaNoCorre() {
        ReporteProgramado semanal = programar(negocio, "Ventas de la semana");
        ejecutarComoElServicio(negocio, "update reportes_programados set proxima_ejecucion = now(), "
                + "activo = false where id = '" + semanal.id() + "'");

        assertThat(enContexto(negocio, contador, CONTADOR, () -> programaciones.barrer())).isZero();
    }

    @Test
    @DisplayName("El barrido de un negocio no dispara las programaciones del otro")
    void aislamientoDelBarrido() {
        ReporteProgramado delOtro = programar(otroNegocio, "Ventas del vecino");
        ejecutarComoElServicio(otroNegocio, "update reportes_programados set proxima_ejecucion = now() "
                + "where id = '" + delOtro.id() + "'");

        assertThat(enContexto(negocio, contador, CONTADOR, () -> programaciones.barrer())).isZero();
        assertThat(enContexto(otroNegocio, contador, CONTADOR, () -> programaciones.barrer()))
                .isEqualTo(1);
    }
}
