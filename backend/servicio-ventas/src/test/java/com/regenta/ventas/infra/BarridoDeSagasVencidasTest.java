package com.regenta.ventas.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.regenta.comun.barridos.EjecutorDeBarridos;
import com.regenta.comun.barridos.ResultadoDeBarrido;
import com.regenta.ventas.BaseDeVentas;
import com.regenta.ventas.aplicacion.GestionDeVentas;
import com.regenta.ventas.aplicacion.SagaDeConfirmacionDeVenta;
import com.regenta.ventas.aplicacion.SolicitudDeLinea;
import com.regenta.ventas.aplicacion.SolicitudDeVenta;

/**
 * HU-124 con la saga de stock: el timeout se compensa solo, en todos los
 * negocios, sin que nadie llame al endpoint.
 */
@TestPropertySource(properties = {
        "regenta.barridos.usuario=" + BaseDeVentas.ROL_BARRIDOS,
        "regenta.barridos.clave=" + BaseDeVentas.CLAVE,
        // El reloj apagado: el test barre cuando quiere, no cuando pasa el tiempo.
        "regenta.barridos.programados=false"})
class BarridoDeSagasVencidasTest extends BaseDeVentas {

    private static final Set<String> SETUP =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");

    @Autowired
    private EjecutorDeBarridos barridos;
    @Autowired
    private GestionDeVentas ventas;
    @Autowired
    private SagaDeConfirmacionDeVenta saga;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    /** Una venta cuya saga ya venció esperando a Inventario. */
    private UUID ventaVencida(UUID negocio) {
        return enContexto(negocio, usuario, SETUP, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(UUID.randomUUID(), null, null,
                    "MOSTRADOR")).id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Prod", "UND",
                    BigDecimal.ONE, new BigDecimal("100"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("40")));
            saga.iniciar(id, Duration.ofSeconds(-1));
            return id;
        });
    }

    private String estadoVenta(UUID negocio, UUID venta) {
        return comoElServicio(negocio, "select estado from ventas where id = '" + venta + "'").get(0);
    }

    /** Solo lo de los negocios de este test: otras clases dejan sagas vencidas en la misma base. */
    private int deEsteTest(ResultadoDeBarrido resultado) {
        return resultado.tomadasPorNegocio().entrySet().stream()
                .filter(e -> e.getKey().equals(negocioA) || e.getKey().equals(negocioB))
                .mapToInt(Map.Entry::getValue).sum();
    }

    @Test
    @DisplayName("Criterios 1 y 2: el barrido ve las sagas vencidas de todos los negocios y compensa cada una en el suyo")
    void compensaEnTodosLosNegocios() {
        UUID deA = ventaVencida(negocioA);
        UUID deB = ventaVencida(negocioB);

        ResultadoDeBarrido resultado = barridos.ejecutar("sagas-vencidas");

        assertThat(resultado.tomadasPorNegocio()).containsEntry(negocioA, 1).containsEntry(negocioB, 1);
        assertThat(resultado.fallos()).isEmpty();
        assertThat(estadoVenta(negocioA, deA)).isEqualTo("BORRADOR");
        assertThat(estadoVenta(negocioB, deB)).isEqualTo("BORRADOR");
        assertThat(comoElServicio(negocioA, "select count(*) from ventas where id = '" + deB + "'"))
                .as("cada negocio sigue sin ver lo del otro").containsExactly("0");
    }

    @Test
    @DisplayName("Criterio 3: dos instancias barriendo a la vez no compensan la misma saga dos veces")
    void dosInstanciasNoTomanLaMismaFila() throws Exception {
        List<UUID> vencidas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            vencidas.add(ventaVencida(i % 2 == 0 ? negocioA : negocioB));
        }
        CountDownLatch largada = new CountDownLatch(1);
        CompletableFuture<ResultadoDeBarrido> una = CompletableFuture.supplyAsync(() -> {
            esperar(largada);
            return barridos.ejecutar("sagas-vencidas");
        });
        CompletableFuture<ResultadoDeBarrido> otra = CompletableFuture.supplyAsync(() -> {
            esperar(largada);
            return barridos.ejecutar("sagas-vencidas");
        });
        largada.countDown();

        assertThat(deEsteTest(una.get()) + deEsteTest(otra.get()))
                .as("cada saga la tomó una sola instancia").isEqualTo(6);
        for (int i = 0; i < vencidas.size(); i++) {
            assertThat(estadoVenta(i % 2 == 0 ? negocioA : negocioB, vencidas.get(i)))
                    .isEqualTo("BORRADOR");
        }
    }

    @Test
    @DisplayName("Criterio 5: el endpoint manual sigue barriendo solo el negocio de quien lo llama")
    void elBarridoManualSigueFuncionando() {
        UUID deA = ventaVencida(negocioA);
        UUID deB = ventaVencida(negocioB);

        int compensadas = enContexto(negocioA, usuario, SETUP, () -> saga.compensarVencidas());

        assertThat(compensadas).isEqualTo(1);
        assertThat(estadoVenta(negocioA, deA)).isEqualTo("BORRADOR");
        assertThat(estadoVenta(negocioB, deB)).isEqualTo("PENDIENTE_STOCK");
    }

    private static void esperar(CountDownLatch largada) {
        try {
            largada.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
