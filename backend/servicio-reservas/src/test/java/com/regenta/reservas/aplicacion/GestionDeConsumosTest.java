package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/** HU-073. Cargar consumos a la estancia, con dos negocios cargados. */
class GestionDeConsumosTest extends BaseDeReservas {

    private static final java.util.Set<String> RECEPCION = java.util.Set.of(
            "RESERVAS_RESERVA_VER", "RESERVAS_RESERVA_CREAR", "RESERVAS_RESERVA_EDITAR");

    @Autowired
    private GestionDeReservas reservas;
    @Autowired
    private GestionDeEstancias estancias;
    @Autowired
    private CatalogoDeRecursosStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        catalogo.reiniciar();
    }

    private OffsetDateTime enDias(int dias) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(dias).withNano(0);
    }

    /** Reserva confirmada y con check-in hecho: devuelve el id de la reserva. */
    private UUID estanciaEnCurso(UUID negocio) {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = enContexto(negocio, recepcion, RECEPCION, () -> reservas.crear(
                new SolicitudDeReserva(enDias(5), enDias(8), tipo, recurso, null, null, 2, 0, null,
                        "MOSTRADOR", null)));
        enContexto(negocio, recepcion, RECEPCION, () -> reservas.confirmar(r.id()));
        enContexto(negocio, recepcion, RECEPCION, () -> estancias.checkIn(r.id(),
                new SolicitudDeCheckIn(null, null, null, List.of())));
        return r.id();
    }

    private SolicitudDeConsumo consumo(String origen, UUID productoId, UUID comandaId, String desc,
            String cantidad, String precio, String impuesto) {
        return new SolicitudDeConsumo(origen, productoId, comandaId, desc, new BigDecimal(cantidad),
                new BigDecimal(precio), new BigDecimal(impuesto));
    }

    @Test
    @DisplayName("Criterio 1: cargar un consumo suma al total de la estancia y queda con su origen y fecha")
    void cargaSumaAlSaldo() {
        UUID reservaId = estanciaEnCurso(negocioA);

        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.cargarConsumo(reservaId,
                        consumo("MINIBAR", null, null, "Gaseosa", "2", "12000", "0.19")));

        assertThat(e.consumoTotal()).isEqualByComparingTo("28560");
        assertThat(e.consumos()).singleElement().satisfies(c -> {
            assertThat(c.origen()).isEqualTo("MINIBAR");
            assertThat(c.total()).isEqualByComparingTo("28560");
            assertThat(c.cargadoEn()).isNotNull();
        });
        // saldoConConsumos = saldo de la reserva + lo cargado.
        BigDecimal saldoReserva = enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.ver(reservaId)).saldo();
        assertThat(e.saldoConConsumos()).isEqualByComparingTo(saldoReserva.add(new BigDecimal("28560")));
    }

    @Test
    @DisplayName("Criterio 2: un consumo enlazado a un producto guarda su producto_id para el descuento de stock")
    void consumoConProducto() {
        UUID reservaId = estanciaEnCurso(negocioA);
        UUID producto = UUID.randomUUID();

        enContexto(negocioA, recepcion, RECEPCION, () -> estancias.cargarConsumo(reservaId,
                consumo("MINIBAR", producto, null, "Agua", "1", "5000", "0")));

        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.verEstancia(reservaId));
        assertThat(e.consumos()).singleElement()
                .satisfies(c -> assertThat(c.productoId()).isEqualTo(producto));
    }

    @Test
    @DisplayName("Criterio 3: una comanda de restaurante cargada a la habitación queda enlazada por su id")
    void comandaEnlazada() {
        UUID reservaId = estanciaEnCurso(negocioA);
        UUID comanda = UUID.randomUUID();

        enContexto(negocioA, recepcion, RECEPCION, () -> estancias.cargarConsumo(reservaId,
                consumo("RESTAURANTE", null, comanda, "Cena para 2", "1", "85000", "0.08")));

        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.verEstancia(reservaId));
        assertThat(e.consumos()).singleElement().satisfies(c -> {
            assertThat(c.origen()).isEqualTo("RESTAURANTE");
            assertThat(c.comandaId()).isEqualTo(comanda);
        });
    }

    @Test
    @DisplayName("Criterio 4: cargar un consumo a una estancia ya cerrada responde 409")
    void estanciaCerrada() {
        UUID reservaId = estanciaEnCurso(negocioA);
        ejecutarComoElServicio(negocioA,
                "UPDATE estancias SET estado = 'FINALIZADA' WHERE reserva_id = '" + reservaId + "'");

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.cargarConsumo(reservaId,
                        consumo("SPA", null, null, "Masaje", "1", "60000", "0"))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Varios consumos se acumulan en el total de la estancia")
    void variosConsumosSeAcumulan() {
        UUID reservaId = estanciaEnCurso(negocioA);

        enContexto(negocioA, recepcion, RECEPCION, () -> estancias.cargarConsumo(reservaId,
                consumo("MINIBAR", null, null, "Gaseosa", "1", "10000", "0")));
        EstanciaDelNegocio e = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.cargarConsumo(reservaId,
                        consumo("LAVANDERIA", null, null, "Camisas", "3", "8000", "0")));

        assertThat(e.consumoTotal()).isEqualByComparingTo("34000");
        assertThat(e.consumos()).hasSize(2);
    }

    @Test
    @DisplayName("Cargar un consumo a una reserva sin estancia responde 404")
    void sinEstancia() {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = enContexto(negocioA, recepcion, RECEPCION, () -> reservas.crear(
                new SolicitudDeReserva(enDias(20), enDias(22), tipo, recurso, null, null, 2, 0,
                        null, "MOSTRADOR", null)));

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.cargarConsumo(r.id(),
                        consumo("MINIBAR", null, null, "Gaseosa", "1", "10000", "0"))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no puede cargar consumos a la estancia del primero")
    void aislamientoEntreNegocios() {
        UUID reservaId = estanciaEnCurso(negocioA);

        assertThatThrownBy(() -> enContexto(negocioB, recepcion, RECEPCION,
                () -> estancias.cargarConsumo(reservaId,
                        consumo("MINIBAR", null, null, "Gaseosa", "1", "10000", "0"))))
                .isInstanceOf(NoEncontradoException.class);
    }
}
