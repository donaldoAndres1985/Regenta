package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

/** HU-074. Check-out: liquidación, cierre y evento de finalización, con dos negocios. */
class GestionDeCheckOutTest extends BaseDeReservas {

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

    /** Reserva por 2 noches (total 200000), confirmada y con check-in hecho. */
    private ReservaDelNegocio conCheckIn(UUID negocio) {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = enContexto(negocio, recepcion, RECEPCION, () -> reservas.crear(
                new SolicitudDeReserva(enDias(5), enDias(7), tipo, recurso, null, null, 2, 0, null,
                        "MOSTRADOR", null)));
        enContexto(negocio, recepcion, RECEPCION, () -> reservas.confirmar(r.id()));
        enContexto(negocio, recepcion, RECEPCION, () -> estancias.checkIn(r.id(),
                new SolicitudDeCheckIn(null, null, null, List.of())));
        return r;
    }

    private void cargar(UUID negocio, UUID reservaId, UUID productoId, String total) {
        enContexto(negocio, recepcion, RECEPCION, () -> estancias.cargarConsumo(reservaId,
                new SolicitudDeConsumo("MINIBAR", productoId, null, "Consumo", BigDecimal.ONE,
                        new BigDecimal(total), BigDecimal.ZERO)));
    }

    private void pagar(UUID negocio, UUID reservaId, String tipo, String monto) {
        enContexto(negocio, recepcion, RECEPCION, () -> reservas.registrarPago(reservaId,
                new SolicitudDePagoDeReserva(tipo, "EFECTIVO", new BigDecimal(monto), null, null)));
    }

    @Test
    @DisplayName("Criterio 1: la liquidación suma alojamiento, servicios y consumos, y resta el anticipo")
    void liquidacion() {
        ReservaDelNegocio r = conCheckIn(negocioA);
        cargar(negocioA, r.id(), null, "30000");
        pagar(negocioA, r.id(), "ANTICIPO", "50000");

        LiquidacionDeEstancia liq = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.verLiquidacion(r.id()));

        assertThat(liq.alojamiento()).isEqualByComparingTo("200000");
        assertThat(liq.servicios()).isEqualByComparingTo("0");
        assertThat(liq.consumos()).isEqualByComparingTo("30000");
        assertThat(liq.subtotal()).isEqualByComparingTo("230000");
        assertThat(liq.anticipo()).isEqualByComparingTo("50000");
        assertThat(liq.saldoPendiente()).isEqualByComparingTo("180000");
    }

    @Test
    @DisplayName("Criterio 5: cerrar con saldo pendiente exige confirmación explícita")
    void saldoPendienteExigeConfirmacion() {
        ReservaDelNegocio r = conCheckIn(negocioA);

        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(false, null))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        LiquidacionDeEstancia liq = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));
        assertThat(liq.estadoReserva()).isEqualTo("CHECK_OUT");
        assertThat(liq.estadoEstancia()).isEqualTo("FINALIZADA");
    }

    @Test
    @DisplayName("Criterio 5: un pago que cubre el saldo permite cerrar sin la confirmación")
    void pagoFinalCierraSinConfirmar() {
        ReservaDelNegocio r = conCheckIn(negocioA);

        LiquidacionDeEstancia liq = enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(false,
                        new SolicitudDePagoDeReserva("SALDO", "TARJETA_CREDITO",
                                new BigDecimal("200000"), null, null))));

        assertThat(liq.saldoPendiente()).isEqualByComparingTo("0");
        assertThat(liq.estadoReserva()).isEqualTo("CHECK_OUT");
    }

    @Test
    @DisplayName("Criterios 2 y 3: el check-out publica estancia_finalizada con origen RESERVA y sus líneas")
    void publicaEstanciaFinalizada() {
        ReservaDelNegocio r = conCheckIn(negocioA);
        cargar(negocioA, r.id(), null, "25000");
        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        String payload = consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '"
                + r.id() + "' AND tipo_evento = 'estancia_finalizada'").get(0);
        assertThat(payload).contains("\"origen_tipo\": \"RESERVA\"");
        assertThat(payload).contains("\"origen\": \"ALOJAMIENTO\"");
        assertThat(payload).contains("\"origen\": \"MINIBAR\"");
        assertThat(payload).contains("\"saldo_pendiente\"");
        // subtotal = alojamiento 200000 + consumo 25000
        assertThat(enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.verLiquidacion(r.id())).subtotal()).isEqualByComparingTo("225000");
    }

    @Test
    @DisplayName("Criterio 4: el check-out publica check_out_registrado sugiriendo LIMPIEZA para el recurso")
    void publicaCheckOutConLimpieza() {
        ReservaDelNegocio r = conCheckIn(negocioA);
        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        String payload = consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '"
                + r.id() + "' AND tipo_evento = 'check_out_registrado'").get(0);
        assertThat(payload).contains("\"estado_recurso_sugerido\": \"LIMPIEZA\"");
        assertThat(payload).contains("\"recurso_id\"");
    }

    @Test
    @DisplayName("El check-out deja el evento CHECK_IN -> CHECK_OUT en reserva_eventos")
    void dejaEvento() {
        ReservaDelNegocio r = conCheckIn(negocioA);
        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        assertThat(contar("SELECT count(*) FROM reserva_eventos WHERE reserva_id = '" + r.id()
                + "' AND estado_anterior = 'CHECK_IN' AND estado_nuevo = 'CHECK_OUT'")).isEqualTo(1);
    }

    @Test
    @DisplayName("Un consumo con producto publica insumos_consumidos para el descuento de stock")
    void publicaInsumosConsumidos() {
        ReservaDelNegocio r = conCheckIn(negocioA);
        UUID producto = UUID.randomUUID();
        cargar(negocioA, r.id(), producto, "8000");
        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        String payload = consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '"
                + r.id() + "' AND tipo_evento = 'insumos_consumidos'").get(0);
        assertThat(payload).contains("\"producto_id\": \"" + producto + "\"");
    }

    @Test
    @DisplayName("No se hace check-out dos veces ni de una reserva sin check-in")
    void checkOutInvalido() {
        ReservaDelNegocio r = conCheckIn(negocioA);
        enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));
        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio sinCheckIn = enContexto(negocioA, recepcion, RECEPCION,
                () -> reservas.crear(new SolicitudDeReserva(enDias(20), enDias(22), tipo, recurso,
                        null, null, 2, 0, null, "MOSTRADOR", null)));
        enContexto(negocioA, recepcion, RECEPCION, () -> reservas.confirmar(sinCheckIn.id()));
        assertThatThrownBy(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(sinCheckIn.id(), new SolicitudDeCheckOut(true, null))))
                .isInstanceOf(NoEncontradoException.class); // no tiene estancia
    }

    @Test
    @DisplayName("El segundo negocio no puede hacer check-out de la reserva del primero")
    void aislamientoEntreNegocios() {
        UUID enA = conCheckIn(negocioA).id();
        assertThatThrownBy(() -> enContexto(negocioB, recepcion, RECEPCION,
                () -> estancias.checkOut(enA, new SolicitudDeCheckOut(true, null))))
                .isInstanceOf(NoEncontradoException.class);
        assertThatCode(() -> enContexto(negocioA, recepcion, RECEPCION,
                () -> estancias.checkOut(enA, new SolicitudDeCheckOut(true, null))))
                .doesNotThrowAnyException();
    }
}
