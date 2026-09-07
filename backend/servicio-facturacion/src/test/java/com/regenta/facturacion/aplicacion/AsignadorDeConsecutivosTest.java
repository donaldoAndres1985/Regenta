package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.domain.TipoDocumento;

/** HU-054. Asignación del consecutivo dentro del rango autorizado. */
class AsignadorDeConsecutivosTest extends BaseDeFacturacion {

    private static final Set<String> DE_ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR");
    private static final Set<String> AL_EMITIR = Set.of();

    @Autowired
    private AsignadorDeConsecutivos asignador;

    @Autowired
    private GestionDeResoluciones resoluciones;

    @Autowired
    private PlatformTransactionManager txManager;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID cargarRango(UUID negocio, String numero, long desde, long hasta) {
        return enContexto(negocio, admin, DE_ADMIN, () -> resoluciones.cargar(
                new SolicitudDeResolucion(null, "FACTURA_VENTA", numero, "FE", desde, hasta, null,
                        LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")))
                .id();
    }

    private ConsecutivoAsignado asignar(UUID negocio) {
        return enContexto(negocio, admin, AL_EMITIR,
                () -> asignador.asignar(TipoDocumento.FACTURA_VENTA, null));
    }

    @Test
    @DisplayName("Criterios 1 y 2: los consecutivos salen continuos, con prefijo y sin huecos")
    void consecutivosContinuos() {
        cargarRango(negocioA, "R-1", 100, 110);

        List<Long> numeros = IntStream.range(0, 5).mapToObj(i -> asignar(negocioA).numero()).toList();

        assertThat(numeros).containsExactly(100L, 101L, 102L, 103L, 104L);
        assertThat(asignar(negocioA).numeroCompleto()).isEqualTo("FE105");
        assertThat(comoElServicio(negocioA,
                "select consecutivo_actual from resoluciones where numero_resolucion = 'R-1'"))
                .containsExactly("106");
    }

    @Test
    @DisplayName("Criterio 3: al tomar el último del rango la resolución queda AGOTADA y no se puede seguir")
    void seAgota() {
        UUID id = cargarRango(negocioA, "R-2", 500, 502);

        assertThat(asignar(negocioA).agotada()).isFalse();   // 500
        assertThat(asignar(negocioA).agotada()).isFalse();   // 501
        assertThat(asignar(negocioA).agotada()).isTrue();    // 502, último

        assertThatThrownBy(() -> asignar(negocioA)).isInstanceOf(NoEncontradoException.class);

        assertThat(comoElServicio(negocioA,
                "select estado || '|' || consecutivo_actual from resoluciones where id = '" + id + "'"))
                .containsExactly("AGOTADA|503");
    }

    @Test
    @DisplayName("Criterio 4: si la transacción hace rollback, el consecutivo no se consume")
    void rollbackNoConsume() {
        UUID id = cargarRango(negocioA, "R-3", 700, 710);
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThatThrownBy(() -> enContexto(negocioA, admin, AL_EMITIR, (Runnable) () ->
                tx.executeWithoutResult(status -> {
                    asignador.asignar(TipoDocumento.FACTURA_VENTA, null);
                    throw new IllegalStateException("rollback a propósito");
                })))
                .isInstanceOf(IllegalStateException.class);

        assertThat(comoElServicio(negocioA,
                "select consecutivo_actual from resoluciones where id = '" + id + "'"))
                .containsExactly("700");
    }

    @Test
    @DisplayName("50 emisiones simultáneas: 50 números distintos y consecutivos, sin huecos ni repetidos")
    void cincuentaConcurrentes() throws Exception {
        cargarRango(negocioA, "R-4", 1000, 1100);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Callable<Long>> tareas = IntStream.range(0, 50)
                    .<Callable<Long>>mapToObj(i -> () -> asignar(negocioA).numero())
                    .toList();
            List<Future<Long>> futuros = pool.invokeAll(tareas);

            List<Long> numeros = futuros.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).sorted().toList();

            assertThat(numeros).hasSize(50).doesNotHaveDuplicates();
            assertThat(numeros)
                    .isEqualTo(IntStream.rangeClosed(1000, 1049).asLongStream().boxed().toList());
            assertThat(comoElServicio(negocioA,
                    "select consecutivo_actual from resoluciones where numero_resolucion = 'R-4'"))
                    .containsExactly("1050");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("La resolución de un negocio no se usa desde otro")
    void aisladoPorNegocio() {
        cargarRango(negocioA, "R-5", 1, 100);

        assertThatThrownBy(() -> asignar(negocioB)).isInstanceOf(NoEncontradoException.class);
    }
}
