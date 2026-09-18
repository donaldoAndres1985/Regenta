package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
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

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.ventas.BaseDeVentas;

/**
 * El snapshot del cliente viaja en {@code venta_completada}
 * (`design/comportamiento/ClienteVenta.md`, R9).
 *
 * <p>Facturación no puede consultar la base de Ventas ni la de Clientes: arma
 * la factura con lo que trae el evento. Si el snapshot no va ahí, la factura
 * sale sin adquiriente y la DIAN la rechaza.
 */
@Import(SnapshotDelClienteEnElEventoTest.Dobles.class)
class SnapshotDelClienteEnElEventoTest extends BaseDeVentas {

    private static final Set<String> VENDEDOR = Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER",
            "VENTAS_VENTA_CONFIRMAR");

    @TestConfiguration
    static class Dobles {
        @Bean
        @Primary
        ConsultaDeClientesDoble consultaDeClientesDoble() {
            return new ConsultaDeClientesDoble();
        }
    }

    static class ConsultaDeClientesDoble implements ConsultaDeClientes {
        final Map<UUID, ClienteDeLaVenta> conocidos = new HashMap<>();

        @Override
        public ClienteDeLaVenta consultar(UUID clienteId) {
            ClienteDeLaVenta cliente = conocidos.get(clienteId);
            if (cliente == null) {
                throw new NoEncontradoException("Ese cliente no existe");
            }
            return cliente;
        }
    }

    @Autowired
    private GestionDeVentas ventas;
    @Autowired
    private GestionDeClienteDeLaVenta clienteDeLaVenta;
    @Autowired
    private SagaDeConfirmacionDeVenta saga;
    @Autowired
    private ConsultaDeClientesDoble clientes;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private final UUID ferreteria = UUID.randomUUID();

    private UUID venta;

    @BeforeEach
    void preparar() {
        clientes.conocidos.clear();
        clientes.conocidos.put(ferreteria, new ClienteDeLaVenta(ferreteria,
                "Materiales Cruz S.A.S.", "NIT", "900412883", "1"));
        venta = enContexto(negocio, usuario, VENDEDOR, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR")).id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Martillo", "UND",
                    BigDecimal.ONE, new BigDecimal("32000"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("12000")));
            return id;
        });
    }

    private String eventoDeVentaCompletada() {
        UUID correlacion = UUID.fromString(comoElServicio(negocio,
                "select correlacion_id from sagas where agregado_id = '" + venta + "'").get(0));
        enContexto(negocio, usuario, VENDEDOR, () -> saga.alStockReservado(correlacion));
        return comoElServicio(negocio, "select payload from outbox_eventos where agregado_id = '"
                + venta + "' and tipo_evento = 'venta_completada'").get(0);
    }

    @Test
    @DisplayName("R9: con cliente, venta_completada lleva el snapshot que la venta congeló")
    void conClienteElEventoLlevaElSnapshot() {
        enContexto(negocio, usuario, VENDEDOR, () -> clienteDeLaVenta.asignar(venta, ferreteria));
        enContexto(negocio, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        String payload = eventoDeVentaCompletada();

        assertThat(payload)
                .contains("\"cliente_id\": \"" + ferreteria + "\"")
                .contains("Materiales Cruz S.A.S.")
                .contains("900412883");
    }

    @Test
    @DisplayName("R8: sin cliente el evento va sin snapshot, y eso no es un error")
    void sinClienteElEventoVaSinSnapshot() {
        enContexto(negocio, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        String payload = eventoDeVentaCompletada();

        assertThat(payload)
                .as("consumidor final se arma al facturar, no se inventa aquí")
                .contains("\"cliente_id\": null")
                .contains("\"cliente_snapshot\": null");
    }
}
