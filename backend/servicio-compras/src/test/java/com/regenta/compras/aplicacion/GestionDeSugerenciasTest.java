package com.regenta.compras.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
import com.regenta.compras.BaseDeCompras;
import com.regenta.compras.infra.ConsumidorDeStockBajo;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-050. Sugerencia de compra a partir de stock bajo mínimo. */
class GestionDeSugerenciasTest extends BaseDeCompras {

    private static final Set<String> COMPRADOR = Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR",
            "COMPRAS_COMPRA_APROBAR", "COMPRAS_PROVEEDOR_VER", "COMPRAS_PROVEEDOR_CREAR",
            "COMPRAS_PROVEEDOR_EDITAR");

    @Autowired
    private ConsumidorDeStockBajo consumidor;
    @Autowired
    private GestionDeSugerencias sugerencias;
    @Autowired
    private GestionDeOrdenesDeCompra ordenes;
    @Autowired
    private GestionDeProveedores proveedores;
    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID nuevoProveedor(UUID negocio, String doc) {
        return enContexto(negocio, usuario, COMPRADOR, () -> proveedores.crear(new SolicitudDeProveedor(
                "NIT", doc, "Proveedor " + doc, null, "C", "p@c.co", "3000000000", "Calle",
                "Bogotá", 30, new BigDecimal("9000000"), 4, null))).id();
    }

    private void asociar(UUID negocio, UUID proveedor, UUID producto, String costo, boolean preferido) {
        enContexto(negocio, usuario, COMPRADOR, () -> proveedores.asociarProducto(proveedor,
                new SolicitudDeProductoDeProveedor(producto, "COD", new BigDecimal(costo), 5,
                        BigDecimal.ONE, preferido)));
    }

    private void stockBajo(UUID negocio, UUID producto, String nombre, int existencia, int minimo,
            int maximo) {
        consumidor.recibir(mensaje(UUID.randomUUID().toString(), Map.of(
                "negocio_id", negocio.toString(),
                "producto_id", producto.toString(),
                "producto_nombre", nombre,
                "existencia", existencia, "minimo", minimo, "stock_maximo", maximo,
                "bodega_id", bodega.toString(), "bodega_nombre", "Central")));
    }

    private Message mensaje(String id, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(id);
            props.setReceivedRoutingKey("stock_bajo_minimo");
            return MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Criterio 1: stock_bajo_minimo mete el producto en la lista de sugerencias")
    void eventoCreaSugerencia() {
        UUID prov = nuevoProveedor(negocioA, "900500001");
        UUID producto = UUID.randomUUID();
        asociar(negocioA, prov, producto, "1000", true);

        stockBajo(negocioA, producto, "Tornillo 1/4", 10, 40, 100);

        assertThat(comoElServicio(negocioA,
                "select count(*) from sugerencias_compra where producto_id = '" + producto
                        + "' and estado = 'PENDIENTE'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select cantidad_sugerida from sugerencias_compra where producto_id = '" + producto
                        + "'").get(0)).startsWith("90"); // 100 - 10
        assertThat(comoElServicio(negocioA,
                "select proveedor_id from sugerencias_compra where producto_id = '" + producto + "'"))
                .containsExactly(prov.toString());
    }

    @Test
    @DisplayName("Criterio 1: el mismo evento repetido no crea otra sugerencia (Inbox)")
    void eventoRepetidoNoDuplica() {
        UUID producto = UUID.randomUUID();
        String id = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of("negocio_id", negocioA.toString(),
                "producto_id", producto.toString(), "producto_nombre", "P",
                "existencia", 1, "minimo", 5, "stock_maximo", 10, "bodega_id", bodega.toString());

        consumidor.recibir(mensaje(id, payload));
        consumidor.recibir(mensaje(id, payload));

        assertThat(comoElServicio(negocioA,
                "select count(*) from sugerencias_compra where producto_id = '" + producto + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 2: el listado agrupa por proveedor preferido")
    void listadoAgrupaPorProveedor() {
        UUID provA = nuevoProveedor(negocioA, "900500011");
        UUID provB = nuevoProveedor(negocioA, "900500012");
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        asociar(negocioA, provA, p1, "1000", true);
        asociar(negocioA, provA, p2, "2000", true);
        asociar(negocioA, provB, p3, "3000", true);

        stockBajo(negocioA, p1, "P1", 2, 10, 20);
        stockBajo(negocioA, p2, "P2", 0, 5, 15);
        stockBajo(negocioA, p3, "P3", 1, 8, 12);

        List<GrupoDeSugerencias> grupos = enContexto(negocioA, usuario, COMPRADOR,
                () -> sugerencias.listar());

        assertThat(grupos).hasSize(2);
        assertThat(grupos).anySatisfy(g -> {
            assertThat(g.proveedorId()).isEqualTo(provA);
            assertThat(g.sugerencias()).hasSize(2);
        });
        assertThat(grupos).anySatisfy(g -> {
            assertThat(g.proveedorId()).isEqualTo(provB);
            assertThat(g.sugerencias()).hasSize(1);
        });
    }

    @Test
    @DisplayName("Criterio 3: aceptar un grupo crea una orden de compra en borrador con esas líneas")
    void aceptarCreaOrden() {
        UUID prov = nuevoProveedor(negocioA, "900500021");
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        asociar(negocioA, prov, p1, "1500", true);
        asociar(negocioA, prov, p2, "2500", true);
        stockBajo(negocioA, p1, "P1", 3, 10, 30);
        stockBajo(negocioA, p2, "P2", 0, 10, 20);

        OrdenDelNegocio orden = enContexto(negocioA, usuario, COMPRADOR,
                () -> sugerencias.aceptar(new SolicitudDeAceptacion(prov, bodega, null)));

        assertThat(orden.estado()).isEqualTo("BORRADOR");
        OrdenDelNegocio conLineas = enContexto(negocioA, usuario, COMPRADOR,
                () -> ordenes.ver(orden.id()));
        assertThat(conLineas.lineas()).hasSize(2);
        assertThat(comoElServicio(negocioA,
                "select count(*) from sugerencias_compra where estado = 'EN_ORDEN' and orden_id = '"
                        + orden.id() + "'"))
                .containsExactly("2");
    }

    @Test
    @DisplayName("Criterio 4: un producto sin proveedor asociado queda marcado y no se puede aceptar")
    void sinProveedorSeMarca() {
        UUID prov = nuevoProveedor(negocioA, "900500031");
        UUID producto = UUID.randomUUID();
        // No se asocia el producto a ningún proveedor.
        stockBajo(negocioA, producto, "Huérfano", 0, 5, 10);

        List<GrupoDeSugerencias> grupos = enContexto(negocioA, usuario, COMPRADOR,
                () -> sugerencias.listar());
        GrupoDeSugerencias sinProv = grupos.get(grupos.size() - 1);
        assertThat(sinProv.sinProveedor()).isTrue();
        assertThat(sinProv.proveedorId()).isNull();
        UUID sugId = sinProv.sugerencias().get(0).id();
        assertThat(sinProv.sugerencias().get(0).sinProveedor()).isTrue();

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> sugerencias.aceptar(new SolicitudDeAceptacion(prov, bodega, List.of(sugId)))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Las sugerencias de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        UUID prov = nuevoProveedor(negocioA, "900500041");
        UUID producto = UUID.randomUUID();
        asociar(negocioA, prov, producto, "1000", true);
        stockBajo(negocioA, producto, "P", 1, 5, 10);

        assertThat(comoElServicio(negocioB, "select count(*) from sugerencias_compra"))
                .containsExactly("0");
        assertThat(enContexto(negocioB, usuario, COMPRADOR, () -> sugerencias.listar())).isEmpty();
    }

    @Test
    @DisplayName("Un segundo stock_bajo_minimo del mismo producto refresca la sugerencia, no la duplica")
    void segundoEventoRefresca() {
        UUID prov = nuevoProveedor(negocioA, "900500051");
        UUID producto = UUID.randomUUID();
        asociar(negocioA, prov, producto, "1000", true);
        stockBajo(negocioA, producto, "P", 8, 10, 20);
        stockBajo(negocioA, producto, "P", 2, 10, 20);

        assertThat(comoElServicio(negocioA,
                "select count(*) from sugerencias_compra where producto_id = '" + producto + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select cantidad_sugerida from sugerencias_compra where producto_id = '" + producto
                        + "'").get(0)).startsWith("18"); // 20 - 2
    }
}
