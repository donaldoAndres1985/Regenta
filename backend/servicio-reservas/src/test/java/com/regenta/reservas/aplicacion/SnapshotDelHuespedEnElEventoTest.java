package com.regenta.reservas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.regenta.reservas.BaseDeReservas;
import com.regenta.reservas.infra.CatalogoDeRecursosStub;

/**
 * HU-118. La estancia manda el snapshot del huésped al facturar.
 *
 * <p>{@code estancia_finalizada} lleva {@code cliente_id} pero no lo que hace
 * falta para facturar; Facturación no puede consultar la base de Reservas ni
 * la de Clientes (`design/comportamiento/ClienteVenta.md`, R9, aplicada al
 * patrón Reserva). Sin el snapshot en el evento, el check-out con huésped
 * identificado factura igual que uno sin cliente.
 */
@Import(SnapshotDelHuespedEnElEventoTest.Dobles.class)
class SnapshotDelHuespedEnElEventoTest extends BaseDeReservas {

    private static final Set<String> RECEPCION = Set.of(
            "RESERVAS_RESERVA_VER", "RESERVAS_RESERVA_CREAR", "RESERVAS_RESERVA_EDITAR");

    @TestConfiguration
    static class Dobles {
        @Bean
        @Primary
        ConsultaDeClientesDoble consultaDeClientesDoble() {
            return new ConsultaDeClientesDoble();
        }
    }

    /** servicio-clientes, sin la red. Lo que no esté aquí, no existe en ese negocio. */
    static class ConsultaDeClientesDoble implements ConsultaDeClientes {
        final Map<UUID, ClienteDeLaReserva> conocidos = new HashMap<>();
        boolean fallar = false;

        @Override
        public Optional<ClienteDeLaReserva> consultar(UUID clienteId) {
            if (fallar) {
                return Optional.empty();
            }
            return Optional.ofNullable(conocidos.get(clienteId));
        }
    }

    @Autowired
    private GestionDeReservas reservas;
    @Autowired
    private GestionDeEstancias estancias;
    @Autowired
    private CatalogoDeRecursosStub catalogo;
    @Autowired
    private ConsultaDeClientesDoble clientes;

    private final UUID negocio = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();
    private final UUID tipo = UUID.randomUUID();
    private final UUID huesped = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        catalogo.reiniciar();
        clientes.conocidos.clear();
        clientes.fallar = false;
        clientes.conocidos.put(huesped, new ClienteDeLaReserva(huesped, "María Restrepo Uribe",
                "CC", "43219876", null));
    }

    private OffsetDateTime enDias(int dias) {
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(dias).withNano(0);
    }

    private ReservaDelNegocio conCheckIn(UUID clienteId) {
        UUID recurso = UUID.randomUUID();
        ReservaDelNegocio r = enContexto(negocio, recepcion, RECEPCION, () -> reservas.crear(
                new SolicitudDeReserva(enDias(5), enDias(7), tipo, recurso, clienteId, null, 2, 0,
                        null, "MOSTRADOR", null)));
        enContexto(negocio, recepcion, RECEPCION, () -> reservas.confirmar(r.id()));
        enContexto(negocio, recepcion, RECEPCION, () -> estancias.checkIn(r.id(),
                new SolicitudDeCheckIn(null, null, null, List.of())));
        return r;
    }

    private String eventoDeEstanciaFinalizada(UUID reservaId) {
        return consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '" + reservaId
                + "' AND tipo_evento = 'estancia_finalizada'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: con huésped, estancia_finalizada lleva el snapshot con nombre, tipo y número de documento")
    void conClienteElEventoLlevaElSnapshot() {
        ReservaDelNegocio r = conCheckIn(huesped);

        enContexto(negocio, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        String payload = eventoDeEstanciaFinalizada(r.id());
        assertThat(payload)
                .contains("\"cliente_id\": \"" + huesped + "\"")
                .contains("María Restrepo Uribe")
                .contains("\"tipo_documento\": \"CC\"")
                .contains("43219876");
    }

    @Test
    @DisplayName("Criterio 3: sin huésped el evento va sin snapshot, y eso no es un error")
    void sinClienteElEventoVaSinSnapshot() {
        ReservaDelNegocio r = conCheckIn(null);

        enContexto(negocio, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        String payload = eventoDeEstanciaFinalizada(r.id());
        assertThat(payload)
                .as("consumidor final se arma al facturar, no se inventa aquí")
                .contains("\"cliente_id\": null")
                .contains("\"cliente_snapshot\": null");
    }

    @Test
    @DisplayName("Si servicio-clientes no responde, el check-out sigue y el evento va sin snapshot")
    void siFallaLaConsultaElCheckOutNoSeBloquea() {
        ReservaDelNegocio r = conCheckIn(huesped);
        clientes.fallar = true;

        LiquidacionDeEstancia liq = enContexto(negocio, recepcion, RECEPCION,
                () -> estancias.checkOut(r.id(), new SolicitudDeCheckOut(true, null)));

        assertThat(liq.estadoReserva()).isEqualTo("CHECK_OUT");
        String payload = eventoDeEstanciaFinalizada(r.id());
        assertThat(payload).contains("\"cliente_snapshot\": null");
    }
}
