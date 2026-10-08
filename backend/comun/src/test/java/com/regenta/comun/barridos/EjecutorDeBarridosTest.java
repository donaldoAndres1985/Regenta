package com.regenta.comun.barridos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * HU-124. Un barrido recorre todos los negocios con trabajo pendiente y lo
 * hace, en cada uno, dentro del contexto de ese negocio.
 */
class EjecutorDeBarridosTest {

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID negocioC = UUID.randomUUID();

    private EjecutorDeBarridos ejecutorCon(List<UUID> pendientes, Barrido barrido) {
        return new EjecutorDeBarridos(sql -> pendientes, List.of(barrido));
    }

    @Test
    @DisplayName("Criterio 2: cada negocio se procesa en su propio contexto, con los permisos del barrido")
    void cadaNegocioEnSuContexto() {
        List<UUID> vistos = new ArrayList<>();
        Barrido barrido = new Barrido("sagas-vencidas", "select 1", Set.of("VENTAS"),
                Set.of("VENTAS_VENTA_CONFIRMAR"), Duration.ofMinutes(1), () -> {
                    vistos.add(ContextoDeNegocio.negocioActual());
                    assertThat(ContextoDeNegocio.actual().puede("VENTAS_VENTA_CONFIRMAR")).isTrue();
                    return 2;
                });

        ResultadoDeBarrido resultado = ejecutorCon(List.of(negocioA, negocioB), barrido)
                .ejecutar("sagas-vencidas");

        assertThat(vistos).containsExactly(negocioA, negocioB);
        assertThat(ContextoDeNegocio.hay()).as("no queda un negocio pegado al hilo").isFalse();
        assertThat(resultado.tomadas()).isEqualTo(4);
    }

    @Test
    @DisplayName("Criterio 2: un fallo en un negocio no detiene a los demás")
    void unFalloNoDetieneALosDemas() {
        List<UUID> vistos = new ArrayList<>();
        Barrido barrido = new Barrido("entregas", "select 1", Set.of(), Set.of(), Duration.ofMinutes(1),
                () -> {
                    UUID negocio = ContextoDeNegocio.negocioActual();
                    vistos.add(negocio);
                    if (negocio.equals(negocioB)) {
                        throw new IllegalStateException("se cayó el proveedor");
                    }
                    return 1;
                });

        ResultadoDeBarrido resultado = ejecutorCon(List.of(negocioA, negocioB, negocioC), barrido)
                .ejecutar("entregas");

        assertThat(vistos).containsExactly(negocioA, negocioB, negocioC);
        assertThat(resultado.fallos()).containsOnlyKeys(negocioB);
        assertThat(resultado.fallos().get(negocioB)).contains("se cayó el proveedor");
    }

    @Test
    @DisplayName("Criterio 4: queda registro de cuántas filas tomó cada negocio y cuáles fallaron")
    void registroPorNegocio() {
        Map<UUID, Integer> porNegocio = Map.of(negocioA, 3, negocioB, 0);
        Barrido barrido = new Barrido("seguimientos", "select 1", Set.of(), Set.of(),
                Duration.ofMinutes(1), () -> porNegocio.get(ContextoDeNegocio.negocioActual()));

        ResultadoDeBarrido resultado = ejecutorCon(List.of(negocioA, negocioB), barrido)
                .ejecutar("seguimientos");

        assertThat(resultado.barrido()).isEqualTo("seguimientos");
        assertThat(resultado.tomadasPorNegocio()).isEqualTo(porNegocio);
        assertThat(resultado.fallos()).isEmpty();
        assertThat(resultado.fallidos()).isZero();
    }

    @Test
    @DisplayName("Un barrido que no existe es un error de configuración, no un no-op silencioso")
    void barridoDesconocido() {
        EjecutorDeBarridos ejecutor = ejecutorCon(List.of(), new Barrido("a", "select 1", Set.of(),
                Set.of(), Duration.ofMinutes(1), () -> 0));

        assertThatThrownBy(() -> ejecutor.ejecutar("b")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Terminado cuando: la frecuencia de cada barrido es configuración, no una constante")
    void frecuenciaConfigurable() {
        Barrido barrido = new Barrido("sagas-vencidas", "select 1", Set.of(), Set.of(),
                Duration.ofMinutes(1), () -> 0);
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("regenta.barridos.sagas-vencidas.intervalo", "PT15S");

        assertThat(ProgramadorDeBarridos.intervaloDe(barrido, entorno)).isEqualTo(Duration.ofSeconds(15));
        assertThat(ProgramadorDeBarridos.intervaloDe(barrido, new MockEnvironment()))
                .isEqualTo(Duration.ofMinutes(1));
    }
}
