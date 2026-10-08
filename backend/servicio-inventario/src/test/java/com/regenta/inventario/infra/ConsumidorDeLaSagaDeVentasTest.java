package com.regenta.inventario.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.aplicacion.GestionDeBodegas;
import com.regenta.inventario.aplicacion.GestionDeCategorias;
import com.regenta.inventario.aplicacion.GestionDeProductos;
import com.regenta.inventario.aplicacion.LibroMayorDeInventario;
import com.regenta.inventario.aplicacion.SolicitudDeBodega;
import com.regenta.inventario.aplicacion.SolicitudDeCategoria;
import com.regenta.inventario.aplicacion.SolicitudDeMovimiento;
import com.regenta.inventario.aplicacion.SolicitudDeProducto;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/**
 * HU-127. El lado de Inventario de la saga de ventas: hasta ahora la reserva
 * solo se pedía por endpoint. {@code solicitar_reserva_stock} y
 * {@code venta_completada} llegan por el bus.
 */
class ConsumidorDeLaSagaDeVentasTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_RESERVA_GESTIONAR");

    @Autowired
    private ConsumidorDeLaSagaDeVentas consumidor;
    @Autowired
    private GestionDeCategorias categorias;
    @Autowired
    private GestionDeProductos productos;
    @Autowired
    private GestionDeBodegas bodegas;
    @Autowired
    private LibroMayorDeInventario libro;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private UUID producto;
    private UUID bodega;

    @BeforeEach
    void preparar() {
        enContexto(negocioA, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-S", null, "Prod", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    false, false, false, false, null)).id();
            bodega = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            libro.registrar(new SolicitudDeMovimiento(producto, bodega, TipoMovimiento.ENTRADA_COMPRA,
                    new BigDecimal("10"), OrigenMovimiento.CARGA_INICIAL, null, "carga", "carga-10"));
            return null;
        });
    }

    private Message mensaje(String id, String tipo, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(id);
            props.setReceivedRoutingKey(tipo);
            props.setHeader("negocio_id", payload.get("negocio_id"));
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> solicitud(UUID negocio, UUID venta, String cantidad) {
        return Map.of("negocio_id", negocio.toString(), "origen_tipo", "VENTA",
                "origen_id", venta.toString(), "correlacion_id", UUID.randomUUID().toString(),
                "lineas", List.of(Map.of("producto_id", producto.toString(),
                        "bodega_id", bodega.toString(), "cantidad", cantidad)));
    }

    private List<String> estadoDeReservas(UUID venta) {
        return comoElServicio(negocioA, "select estado from reservas_stock where origen_id = '" + venta + "'");
    }

    @Test
    @DisplayName("solicitar_reserva_stock aparta el stock y publica stock_reservado")
    void laSolicitudReserva() {
        UUID venta = UUID.randomUUID();

        consumidor.recibir(mensaje(UUID.randomUUID().toString(), "solicitar_reserva_stock",
                solicitud(negocioA, venta, "4")));

        assertThat(estadoDeReservas(venta)).containsExactly("ACTIVA");
        assertThat(comoElServicio(negocioA, "select tipo_evento from outbox_eventos where agregado_id = '"
                + venta + "'")).containsExactly("stock_reservado");
    }

    @Test
    @DisplayName("Criterio 4 (lado Inventario): la misma solicitud entregada dos veces no reserva dos veces")
    void laSolicitudRepetidaNoReservaDosVeces() {
        UUID venta = UUID.randomUUID();
        String mismoId = UUID.randomUUID().toString();

        consumidor.recibir(mensaje(mismoId, "solicitar_reserva_stock", solicitud(negocioA, venta, "4")));
        consumidor.recibir(mensaje(mismoId, "solicitar_reserva_stock", solicitud(negocioA, venta, "4")));

        assertThat(estadoDeReservas(venta)).hasSize(1);
        assertThat(comoElServicio(negocioA, "select cantidad_reservada from existencias where producto_id = '"
                + producto + "'").get(0)).startsWith("4");
    }

    @Test
    @DisplayName("venta_completada convierte la reserva en salida real")
    void laVentaCompletadaConfirma() {
        UUID venta = UUID.randomUUID();
        consumidor.recibir(mensaje(UUID.randomUUID().toString(), "solicitar_reserva_stock",
                solicitud(negocioA, venta, "4")));

        consumidor.recibir(mensaje(UUID.randomUUID().toString(), "venta_completada",
                Map.of("negocio_id", negocioA.toString(), "venta_id", venta.toString())));

        assertThat(estadoDeReservas(venta)).containsExactly("CONFIRMADA");
        assertThat(comoElServicio(negocioA, "select count(*) from movimientos_inventario where origen_id = '"
                + venta + "' and tipo = 'SALIDA_VENTA'")).containsExactly("1");
    }

    @Test
    @DisplayName("Una solicitud de otro negocio no toca el stock de este")
    void otroNegocioNoTocaElStock() {
        UUID venta = UUID.randomUUID();

        consumidor.recibir(mensaje(UUID.randomUUID().toString(), "solicitar_reserva_stock",
                solicitud(negocioB, venta, "4")));

        assertThat(estadoDeReservas(venta)).isEmpty();
        assertThat(comoElServicio(negocioB, "select tipo_evento from outbox_eventos where agregado_id = '"
                + venta + "'")).as("en su negocio, el producto no existe").containsExactly("stock_reserva_fallida");
    }
}
