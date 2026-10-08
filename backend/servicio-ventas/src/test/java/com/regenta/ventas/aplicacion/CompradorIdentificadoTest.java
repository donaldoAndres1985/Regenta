package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.ventas.BaseDeVentas;
import com.regenta.ventas.infra.ConsumidorDeConfiguracionDelNegocio;

/**
 * HU-137. Desde cierto monto, la venta no se cobra a consumidor final. El
 * monto lo configura cada negocio en servicio-usuarios y llega por
 * {@code configuracion_negocio_actualizada}.
 */
class CompradorIdentificadoTest extends BaseDeVentas {

    private static final Set<String> VENDEDOR =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");

    @Autowired
    private GestionDeVentas ventas;
    @Autowired
    private ConsumidorDeConfiguracionDelNegocio configuracion;
    @Autowired
    private GestionDeVentasOffline offline;
    @Autowired
    private ConfiguracionDelNegocio reglas;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private void configurarMonto(UUID negocio, String monto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("negocio_id", negocio.toString());
        payload.put("razon_social", "La Esquina SAS");
        payload.put("monto_identificar_comprador", monto == null ? null : new BigDecimal(monto));
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey("configuracion_negocio_actualizada");
            configuracion.recibir(MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Una venta en borrador cuyo total es {@code precio} + 19 % de IVA. */
    private UUID ventaDe(UUID negocio, String precio, UUID clienteId) {
        return enContexto(negocio, usuario, VENDEDOR, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(UUID.randomUUID(), clienteId, null,
                    "MOSTRADOR")).id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Taladro", "UND",
                    BigDecimal.ONE, new BigDecimal(precio), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("100")));
            return id;
        });
    }

    private String estado(UUID negocio, UUID venta) {
        return comoElServicio(negocio, "select estado from ventas where id = '" + venta + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 2: una venta que supera el monto sin cliente no se cobra: pide identificar al comprador")
    void sobreElMontoSinClienteNoSeCobra() {
        configurarMonto(negocioA, "1000000");
        UUID venta = ventaDe(negocioA, "900000", null);   // 1.071.000 con IVA

        assertThatThrownBy(() -> enContexto(negocioA, usuario, VENDEDOR, () -> ventas.confirmar(venta)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("identificar al comprador");
        assertThat(estado(negocioA, venta)).isEqualTo("BORRADOR");
    }

    @Test
    @DisplayName("Criterio 2: con el comprador identificado, la misma venta se cobra")
    void conClienteSeCobra() {
        configurarMonto(negocioA, "1000000");
        UUID venta = ventaDe(negocioA, "900000", UUID.randomUUID());

        enContexto(negocioA, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        assertThat(estado(negocioA, venta)).isEqualTo("PENDIENTE_STOCK");
    }

    @Test
    @DisplayName("Criterio 1: el monto aplica a las ventas que no lo superan como siempre")
    void bajoElMontoSeCobraComoSiempre() {
        configurarMonto(negocioA, "1000000");
        UUID venta = ventaDe(negocioA, "800000", null);   // 952.000 con IVA

        enContexto(negocioA, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        assertThat(estado(negocioA, venta)).isEqualTo("PENDIENTE_STOCK");
    }

    @Test
    @DisplayName("Criterio 3: un negocio sin monto configurado cobra como hoy, aunque otro negocio sí lo tenga")
    void sinMontoNoSeExigeNada() {
        configurarMonto(negocioA, "1000000");
        UUID venta = ventaDe(negocioB, "900000", null);

        enContexto(negocioB, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        assertThat(estado(negocioB, venta)).isEqualTo("PENDIENTE_STOCK");
    }

    @Test
    @DisplayName("Criterio 3: quitar el monto vuelve al comportamiento de siempre")
    void quitarElMontoDejaDeExigir() {
        configurarMonto(negocioA, "1000000");
        configurarMonto(negocioA, null);
        UUID venta = ventaDe(negocioA, "900000", null);

        enContexto(negocioA, usuario, VENDEDOR, () -> ventas.confirmar(venta));

        assertThat(estado(negocioA, venta)).isEqualTo("PENDIENTE_STOCK");
    }

    @Test
    @DisplayName("La venta que se cobró sin señal no se rechaza al subir: ya está cobrada")
    void laCobradaSinSenalNoSeRechaza() {
        configurarMonto(negocioA, "1000000");

        VentaDelNegocio subida = enContexto(negocioA, usuario, VENDEDOR, () -> offline.subir(
                new SolicitudDeVentaOffline(UUID.randomUUID(), "celular", OffsetDateTime.now(),
                        UUID.randomUUID(), null, null, "MOSTRADOR",
                        List.of(new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Taladro", "UND",
                                BigDecimal.ONE, new BigDecimal("900000"), BigDecimal.ZERO, "IVA19",
                                new BigDecimal("19"), new BigDecimal("100"))))));

        assertThat(estado(negocioA, subida.id())).isEqualTo("PENDIENTE_STOCK");
    }

    @Test
    @DisplayName("Criterio 4: quien vende puede preguntar el monto, para avisar antes de llegar a Cobrar")
    void elPosPuedePreguntarElMonto() {
        configurarMonto(negocioA, "1000000");

        assertThat(enContexto(negocioA, usuario, VENDEDOR, () -> reglas.reglasDeCobro())
                .montoIdentificarComprador()).isEqualByComparingTo("1000000");
        assertThat(enContexto(negocioB, usuario, VENDEDOR, () -> reglas.reglasDeCobro())
                .montoIdentificarComprador()).as("el de otro negocio no se ve").isNull();
    }
}
