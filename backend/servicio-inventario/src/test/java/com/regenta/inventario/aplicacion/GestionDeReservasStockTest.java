package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/** HU-034. Reserva y liberación de stock para la saga de ventas. */
class GestionDeReservasStockTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_RESERVA_GESTIONAR");

    @Autowired
    private GestionDeReservasStock reservas;

    @Autowired
    private LibroMayorDeInventario libro;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID producto;
    private UUID bodega;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-R", null, "Prod", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    false, false, false, false, null)).id();
            bodega = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            return null;
        });
    }

    private void sembrar(String cantidad) {
        enContexto(negocio, usuario, SETUP, () -> libro.registrar(new SolicitudDeMovimiento(producto,
                bodega, TipoMovimiento.ENTRADA_COMPRA, new BigDecimal(cantidad),
                OrigenMovimiento.CARGA_INICIAL, null, "carga", "carga-" + cantidad)));
    }

    private ResultadoDeReserva solicitar(UUID origenId, String cantidad, OffsetDateTime expira) {
        return enContexto(negocio, usuario, SETUP, () -> reservas.solicitar(new SolicitudDeReservaStock(
                "VENTA", origenId, UUID.randomUUID(), expira,
                List.of(new LineaDeReserva(producto, bodega, new BigDecimal(cantidad))))));
    }

    private String reservada() {
        return comoElServicio(negocio, "select cantidad_reservada from existencias where producto_id = '"
                + producto + "' and bodega_id = '" + bodega + "'").stream().findFirst().orElse("0");
    }

    private List<String> eventosDeReserva() {
        return comoElServicio(negocio, "select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento like 'stock_reserv%' order by creado_en");
    }

    @Test
    @DisplayName("Criterio 1: con stock suficiente se crea la reserva, sube cantidad_reservada y se publica stock_reservado")
    void reservaConStockSuficiente() {
        sembrar("10");
        UUID venta = UUID.randomUUID();

        ResultadoDeReserva r = solicitar(venta, "4", null);

        assertThat(r.reservada()).isTrue();
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("4");
        assertThat(comoElServicio(negocio, "select estado from reservas_stock where origen_id = '"
                + venta + "'")).containsExactly("ACTIVA");
        assertThat(eventosDeReserva()).containsExactly("stock_reservado");
    }

    @Test
    @DisplayName("Criterio 2: con stock insuficiente se publica stock_reserva_fallida y no se reserva nada")
    void reservaConStockInsuficiente() {
        sembrar("3");
        UUID venta = UUID.randomUUID();

        ResultadoDeReserva r = solicitar(venta, "5", null);

        assertThat(r.reservada()).isFalse();
        assertThat(r.faltantes()).singleElement().satisfies(f -> {
            assertThat(f.solicitado()).isEqualByComparingTo("5");
            assertThat(f.disponible()).isEqualByComparingTo("3");
        });
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("0");
        assertThat(comoElServicio(negocio, "select count(*) from reservas_stock")).containsExactly("0");
        assertThat(eventosDeReserva()).containsExactly("stock_reserva_fallida");
    }

    @Test
    @DisplayName("Criterio 3: una reserva que expira sin confirmarse la libera el barrido y baja cantidad_reservada")
    void reservaExpiradaSeLibera() {
        sembrar("10");
        UUID venta = UUID.randomUUID();
        solicitar(venta, "6", OffsetDateTime.now().minusMinutes(1));
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("6");

        int liberadas = enContexto(negocio, usuario, SETUP, () -> reservas.liberarExpiradas());

        assertThat(liberadas).isEqualTo(1);
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("0");
        assertThat(comoElServicio(negocio, "select estado from reservas_stock where origen_id = '"
                + venta + "'")).containsExactly("EXPIRADA");
    }

    @Test
    @DisplayName("Criterio 4: dos solicitudes concurrentes por la última unidad, solo una obtiene la reserva")
    void dosSolicitudesConcurrentesPorLaUltimaUnidad() throws InterruptedException {
        sembrar("1");
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch arranquen = new CountDownLatch(1);
        AtomicInteger ganadas = new AtomicInteger();
        AtomicReference<Throwable> fallo = new AtomicReference<>();

        Runnable intento = () -> {
            listos.countDown();
            try {
                arranquen.await();
                ResultadoDeReserva r = solicitar(UUID.randomUUID(), "1", null);
                if (r.reservada()) {
                    ganadas.incrementAndGet();
                }
            } catch (Throwable t) {
                fallo.set(t);
            }
        };

        Thread a = new Thread(intento);
        Thread b = new Thread(intento);
        a.start();
        b.start();
        listos.await(5, TimeUnit.SECONDS);
        arranquen.countDown();
        a.join(10_000);
        b.join(10_000);

        assertThat(fallo.get()).isNull();
        assertThat(ganadas.get()).isEqualTo(1);
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("1");
        assertThat(comoElServicio(negocio,
                "select count(*) from reservas_stock where estado = 'ACTIVA'")).containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 5: al llegar venta_completada la reserva se convierte en salida real con su movimiento")
    void reservaConfirmadaSeConvierteEnSalida() {
        sembrar("10");
        UUID venta = UUID.randomUUID();
        solicitar(venta, "4", null);

        enContexto(negocio, usuario, SETUP, () -> {
            reservas.confirmar("VENTA", venta);
            return null;
        });

        assertThat(comoElServicio(negocio, "select estado from reservas_stock where origen_id = '"
                + venta + "'")).containsExactly("CONFIRMADA");
        assertThat(comoElServicio(negocio, "select cantidad from existencias where producto_id = '"
                + producto + "' and bodega_id = '" + bodega + "'")).containsExactly("6.000000");
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("0");
        assertThat(comoElServicio(negocio, "select tipo from movimientos_inventario "
                + "where origen_tipo = 'SAGA'")).containsExactly("SALIDA_VENTA");
        assertThat(comoElServicio(negocio, "select cantidad from movimientos_inventario "
                + "where origen_tipo = 'SAGA'")).containsExactly("4.000000");
    }

    @Test
    @DisplayName("La misma solicitud de reserva dos veces no reserva dos veces")
    void solicitudIdempotente() {
        sembrar("10");
        UUID venta = UUID.randomUUID();

        solicitar(venta, "4", null);
        ResultadoDeReserva segunda = solicitar(venta, "4", null);

        assertThat(segunda.reservada()).isTrue();
        assertThat(new BigDecimal(reservada())).isEqualByComparingTo("4");
        assertThat(comoElServicio(negocio, "select count(*) from reservas_stock where origen_id = '"
                + venta + "'")).containsExactly("1");
    }

    @Test
    @DisplayName("Las reservas de otro negocio no se mezclan")
    void aisladoPorNegocio() {
        sembrar("10");
        solicitar(UUID.randomUUID(), "3", null);

        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from reservas_stock"))
                .containsExactly("0");
    }
}
