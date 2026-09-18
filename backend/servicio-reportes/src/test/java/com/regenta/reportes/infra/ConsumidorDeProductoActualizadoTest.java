package com.regenta.reportes.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regenta.reportes.BaseDeReportes;

/**
 * HU-096 criterios 2 y 3. producto_actualizado versiona dim_producto (SCD2); un
 * hecho ya insertado con la versión anterior sigue mostrando el nombre que el
 * producto tenía entonces.
 */
class ConsumidorDeProductoActualizadoTest extends BaseDeReportes {

    @Autowired
    private ConsumidorDeVentasCompletadas consumidorDeVentas;
    @Autowired
    private ConsumidorDeProductoActualizado consumidorDeProducto;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();

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

    private Message ventaCon(UUID negocio, UUID ventaId, String nombreEsperadoIrrelevante) {
        Map<String, Object> linea = Map.of("producto_id", producto.toString(), "sku", "SKU-1", "nombre",
                "snapshot de la venta, no de la dimension", "cantidad", 1, "precio_unitario", "32000",
                "descuento_valor", "0", "impuesto_valor", "0", "total", "32000", "costo_unitario", "12000");
        Map<String, Object> payload = Map.of("negocio_id", negocio.toString(), "venta_id",
                ventaId.toString(), "numero", "V-0001", "bodega_id", UUID.randomUUID().toString(), "total",
                "32000", "fecha", OffsetDateTime.now().toString(), "lineas", List.of(linea));
        return construir("venta_completada", payload);
    }

    private Message productoActualizado(UUID negocio, String nombre) {
        Map<String, Object> payload = Map.of("negocio_id", negocio.toString(), "producto_id",
                producto.toString(), "sku", "SKU-1", "nombre", nombre, "categoria_nombre", "Fuertes",
                "precio_venta", "32000", "costo", "12000");
        return construir("producto_actualizado", payload);
    }

    @Test
    @DisplayName("Criterio 2: cambiar el nombre del producto cierra la versión y abre una nueva")
    void versionaLaDimension() {
        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa"));
        assertThat(contar("select count(*) from dim_producto where producto_id = '" + producto + "'"))
                .isEqualTo(1);

        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa XL"));

        assertThat(contar("select count(*) from dim_producto where producto_id = '" + producto + "'"))
                .isEqualTo(2);
        assertThat(contar("select count(*) from dim_producto where producto_id = '" + producto
                + "' and es_actual")).isEqualTo(1);
        assertThat(comoElServicio(negocioA, "select nombre from dim_producto where producto_id = '"
                + producto + "' and es_actual")).containsExactly("Bandeja paisa XL");
    }

    @Test
    @DisplayName("Sin cambios de verdad, no se abre una versión nueva")
    void sinCambioNoVersiona() {
        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa"));
        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa"));

        assertThat(contar("select count(*) from dim_producto where producto_id = '" + producto + "'"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Criterio 3: un hecho ya insertado sigue mostrando el nombre que el producto tenía entonces")
    void elHechoViejoNoCambiaDeNombre() {
        UUID ventaVieja = UUID.randomUUID();
        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa"));
        consumidorDeVentas.recibir(ventaCon(negocioA, ventaVieja, "irrelevante"));

        Long skViejo = Long.valueOf(comoElServicio(negocioA,
                "select producto_sk from hechos_venta where venta_id = '" + ventaVieja + "'").get(0));

        // El producto cambia de nombre después de esa venta.
        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa XL"));

        assertThat(comoElServicio(negocioA, "select nombre from dim_producto where sk = " + skViejo))
                .containsExactly("Bandeja paisa");
        assertThat(comoElServicio(negocioA,
                "select nombre from dim_producto where producto_id = '" + producto + "' and es_actual"))
                .containsExactly("Bandeja paisa XL");
    }

    @Test
    @DisplayName("El segundo negocio no ve la dimensión del primero")
    void aislamiento() {
        consumidorDeProducto.recibir(productoActualizado(negocioA, "Bandeja paisa"));
        assertThat(comoElServicio(negocioB,
                "select count(*) from dim_producto where producto_id = '" + producto + "'"))
                .containsExactly("0");
    }
}
