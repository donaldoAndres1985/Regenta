package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * HU-113. Quién compra en esta venta.
 *
 * <p>Lo que hace que el histórico no se corrompa es el snapshot: {@code
 * crm.clientes} cambia, la venta no. Reglas en
 * {@code design/comportamiento/ClienteVenta.md}.
 */
@Import(ClienteDeLaVentaTest.Dobles.class)
class ClienteDeLaVentaTest extends BaseDeVentas {

    private static final Set<String> VENDEDOR =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");

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
                "Ferretería El Tornillo SAS", "NIT", "900123456", "7"));
        venta = enContexto(negocio, usuario, VENDEDOR, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR")).id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Martillo", "UND",
                    BigDecimal.ONE, new BigDecimal("32000"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("12000")));
            return id;
        });
    }

    private String columna(String columna) {
        return comoElServicio(negocio,
                "select " + columna + " from ventas where id = '" + venta + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: una venta nueva nace sin cliente, y eso no es un error")
    void laVentaNaceSinCliente() {
        assertThat(columna("cliente_id")).isNull();
        assertThat(columna("cliente_snapshot")).isNull();
        assertThat(enContexto(negocio, usuario, VENDEDOR, () -> ventas.ver(venta)).clienteId()).isNull();
    }

    @Test
    @DisplayName("Criterio 2: asignar un cliente guarda su id y un snapshot con nombre y documento")
    void asignarGuardaIdYSnapshot() {
        enContexto(negocio, usuario, VENDEDOR, () -> clienteDeLaVenta.asignar(venta, ferreteria));

        assertThat(columna("cliente_id")).isEqualTo(ferreteria.toString());
        assertThat(columna("cliente_snapshot"))
                .contains("Ferretería El Tornillo SAS")
                .contains("NIT")
                .contains("900123456")
                .contains("\"digito_verificacion\": \"7\"");
    }

    @Test
    @DisplayName("Criterio 3: un cliente de otro negocio no se puede asignar por su id")
    void elClienteDeOtroNegocioNoSeAsigna() {
        UUID deOtroNegocio = UUID.randomUUID();

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> clienteDeLaVenta.asignar(venta, deOtroNegocio)))
                .isInstanceOf(NoEncontradoException.class);

        assertThat(columna("cliente_id")).isNull();
    }

    @Test
    @DisplayName("Criterio 4: quitar el cliente vuelve a consumidor final y limpia el crédito")
    void quitarVuelveAConsumidorFinal() {
        enContexto(negocio, usuario, VENDEDOR, () -> clienteDeLaVenta.asignar(venta, ferreteria));
        ejecutarComoElServicio(negocio, "update ventas set forma_pago = 'CREDITO', "
                + "saldo_pendiente = total, fecha_vencimiento = current_date + 30 "
                + "where id = '" + venta + "'");

        enContexto(negocio, usuario, VENDEDOR, () -> clienteDeLaVenta.quitar(venta));

        assertThat(columna("cliente_id")).isNull();
        assertThat(columna("cliente_snapshot")).isNull();
        assertThat(columna("forma_pago"))
                .as("el crédito es del cliente: sin cliente no hay a quién cobrarle")
                .isEqualTo("CONTADO");
        assertThat(columna("fecha_vencimiento")).isNull();
        assertThat(new BigDecimal(columna("saldo_pendiente"))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Criterio 5: si mañana corrigen la razón social, la venta conserva el snapshot")
    void elSnapshotNoCambiaCuandoCambiaElCrm() {
        enContexto(negocio, usuario, VENDEDOR, () -> clienteDeLaVenta.asignar(venta, ferreteria));
        enContexto(negocio, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        // En el CRM le corrigen el nombre.
        clientes.conocidos.put(ferreteria, new ClienteDeLaVenta(ferreteria,
                "Ferretería El Tornillo y Compañía SAS", "NIT", "900123456", "7"));

        assertThat(columna("cliente_snapshot"))
                .contains("Ferretería El Tornillo SAS")
                .doesNotContain("Compañía");
    }

    @Test
    @DisplayName("Una venta que ya no está en borrador no cambia de cliente")
    void laVentaConfirmadaNoCambiaDeCliente() {
        enContexto(negocio, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        assertThatThrownBy(() -> enContexto(negocio, usuario, VENDEDOR,
                () -> clienteDeLaVenta.asignar(venta, ferreteria)))
                .isInstanceOf(com.regenta.comun.errores.ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("La venta de otro negocio no existe, ni para asignarle cliente")
    void aislamientoDeLaVenta() {
        UUID otroNegocio = UUID.randomUUID();

        assertThatThrownBy(() -> enContexto(otroNegocio, usuario, VENDEDOR,
                () -> clienteDeLaVenta.asignar(venta, ferreteria)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
