package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.reportes.BaseDeReportes;
import com.regenta.reportes.infra.ConsumidorDeProductoActualizado;
import com.regenta.reportes.infra.ConsumidorDeStockActualizado;
import com.regenta.reportes.infra.ConsumidorDeVentasCompletadas;

/** HU-098. Reportes de ventas, márgenes y rotación. */
class ReporteDeVentasTest extends BaseDeReportes {

    private static final Set<String> VER_REPORTES = Set.of("REPORTES_REPORTE_VER");

    @Autowired
    private ReporteDeVentas reporte;
    @Autowired
    private ConsumidorDeVentasCompletadas consumidorDeVentas;
    @Autowired
    private ConsumidorDeProductoActualizado consumidorDeProducto;
    @Autowired
    private ConsumidorDeStockActualizado consumidorDeStock;
    @Autowired
    private ObjectMapper json;

    private final UUID negocio = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();
    private final UUID bodegaA = UUID.randomUUID();
    private final UUID bodegaB = UUID.randomUUID();

    private Message construir(String tipo, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(UUID.randomUUID().toString());
            props.setReceivedRoutingKey(tipo);
            return MessageBuilder.withBody(json.writeValueAsBytes(payload)).andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message productoActualizado(String nombre, String categoria, String costo) {
        Map<String, Object> payload = Map.of("negocio_id", negocio.toString(), "producto_id",
                producto.toString(), "sku", "SKU-1", "nombre", nombre, "categoria_nombre", categoria,
                "precio_venta", "32000", "costo", costo);
        return construir("producto_actualizado", payload);
    }

    private Message ventaCompletada(UUID bodegaId, String fechaIso) {
        Map<String, Object> linea = Map.of("producto_id", producto.toString(), "sku", "SKU-1", "nombre",
                "Bandeja paisa", "cantidad", 2, "precio_unitario", "32000", "descuento_valor", "0",
                "impuesto_valor", "0", "total", "64000", "costo_unitario", "12000");
        Map<String, Object> payload = Map.of("negocio_id", negocio.toString(), "venta_id",
                UUID.randomUUID().toString(), "numero", "V-0001", "bodega_id", bodegaId.toString(), "canal",
                "MOSTRADOR", "fecha", fechaIso, "total", "64000", "lineas", List.of(linea));
        return construir("venta_completada", payload);
    }

    @Test
    @DisplayName("Criterio 1: ventas por categoría en un rango de fechas trae monto, unidades y margen")
    void ventasPorCategoria() {
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "12000"));
        consumidorDeVentas.recibir(ventaCompletada(bodegaA, "2026-06-15T12:00:00-05:00"));

        List<VentaPorCategoria> filas = enContexto(negocio, null, VER_REPORTES, () -> reporte.porCategoria(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null));

        assertThat(filas).hasSize(1);
        VentaPorCategoria fila = filas.get(0);
        assertThat(fila.categoria()).isEqualTo("Fuertes");
        assertThat(fila.monto()).isEqualByComparingTo("64000");
        assertThat(fila.unidades()).isEqualByComparingTo("2");
        assertThat(fila.margen()).isEqualByComparingTo("40000");
    }

    @Test
    @DisplayName("Fuera del rango de fechas, la venta no aparece en el reporte")
    void fueraDelRangoNoAparece() {
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "12000"));
        consumidorDeVentas.recibir(ventaCompletada(bodegaA, "2026-06-15T12:00:00-05:00"));

        List<VentaPorCategoria> filas = enContexto(negocio, null, VER_REPORTES, () -> reporte.porCategoria(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null));

        assertThat(filas).isEmpty();
    }

    @Test
    @DisplayName("Criterio 2: el margen usa el costo del momento de la venta, no el actual")
    void margenUsaElCostoDeEntonces() {
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "12000"));
        consumidorDeVentas.recibir(ventaCompletada(bodegaA, "2026-06-15T12:00:00-05:00"));
        // El costo del producto sube después de la venta: el reporte de esa venta no cambia.
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "20000"));

        List<VentaPorCategoria> filas = enContexto(negocio, null, VER_REPORTES, () -> reporte.porCategoria(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null));

        assertThat(filas).hasSize(1);
        // monto 64000 - costo_unitario(12000) * 2 = margen 40000, no 24000.
        assertThat(filas.get(0).margen()).isEqualByComparingTo("40000");
    }

    @Test
    @DisplayName("Criterio 4: filtrar por sucursal deja fuera lo de las demás")
    void filtraPorSucursal() {
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "12000"));
        consumidorDeVentas.recibir(ventaCompletada(bodegaA, "2026-06-15T12:00:00-05:00"));
        consumidorDeVentas.recibir(ventaCompletada(bodegaB, "2026-06-16T12:00:00-05:00"));

        List<VentaPorCategoria> soloA = enContexto(negocio, null, VER_REPORTES, () -> reporte.porCategoria(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), bodegaA));

        assertThat(soloA).hasSize(1);
        assertThat(soloA.get(0).monto()).isEqualByComparingTo("64000");
        assertThat(soloA.get(0).unidades()).isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("Criterio 3: la rotación trae los días sin movimiento de cada producto")
    void rotacionTraeDiasSinMovimiento() {
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "12000"));
        consumidorDeStock.recibir(construir("stock_actualizado", Map.of("negocio_id", negocio.toString(),
                "producto_id", producto.toString(), "bodega_id", bodegaA.toString(), "tipo", "ENTRADA",
                "cantidad", "10", "saldo_posterior", "10")));

        List<RotacionProducto> filas =
                enContexto(negocio, null, VER_REPORTES, () -> reporte.rotacion(null));

        assertThat(filas).anySatisfy(fila -> {
            assertThat(fila.productoId()).isEqualTo(producto);
            assertThat(fila.ultimoMovimiento()).isNotNull();
            assertThat(fila.diasSinMovimiento()).isEqualTo(0L);
        });
    }

    @Test
    @DisplayName("Un producto sin movimientos de inventario sale con días sin movimiento nulos")
    void productoSinMovimientoSaleConNulo() {
        consumidorDeProducto.recibir(productoActualizado("Bandeja paisa", "Fuertes", "12000"));

        List<RotacionProducto> filas =
                enContexto(negocio, null, VER_REPORTES, () -> reporte.rotacion(null));

        assertThat(filas).anySatisfy(fila -> {
            assertThat(fila.productoId()).isEqualTo(producto);
            assertThat(fila.ultimoMovimiento()).isNull();
            assertThat(fila.diasSinMovimiento()).isNull();
        });
    }
}
