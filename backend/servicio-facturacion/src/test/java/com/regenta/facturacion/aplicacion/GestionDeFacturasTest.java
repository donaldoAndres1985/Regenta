package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.regenta.facturacion.BaseDeFacturacion;
import com.regenta.facturacion.infra.ConsumidorDeCierresFacturables;

/** HU-053. Emitir factura desde cualquiera de los tres patrones. */
class GestionDeFacturasTest extends BaseDeFacturacion {

    private static final Set<String> DE_ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR");
    private static final Set<String> VER = Set.of("FACTURACION_FACTURA_VER");

    @Autowired
    private ConsumidorDeCierresFacturables consumidor;

    @Autowired
    private GestionDeFacturas facturas;

    @Autowired
    private GestionDeResoluciones resoluciones;

    @Autowired
    private ObjectMapper json;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private void resolucionEn(UUID negocio) {
        enContexto(negocio, admin, DE_ADMIN, () -> resoluciones.cargar(new SolicitudDeResolucion(
                null, "FACTURA_VENTA", "R-" + negocio, "FE", 1, 100000, "clave-abc",
                LocalDate.parse("2026-01-01"), LocalDate.parse("2030-01-01"), "PRODUCCION")));
    }

    private Map<String, Object> evento(UUID negocio, UUID origenId, String clienteNombre,
            List<Map<String, Object>> lineas) {
        return Map.of(
                "negocio_id", negocio.toString(),
                "venta_id", origenId.toString(),
                "numero", "FV-1",
                "cliente_id", UUID.randomUUID().toString(),
                "emisor", Map.of("razon_social", "Mi Negocio SAS", "nit", "900111222"),
                "cliente", Map.of("razon_social", clienteNombre, "nit", "800333444"),
                "lineas", lineas);
    }

    private static Map<String, Object> linea(String desc, String cant, String precio, String descPct,
            String ivaPct) {
        return Map.of(
                "descripcion", desc,
                "cantidad", cant,
                "precio_unitario", precio,
                "descuento_pct", descPct,
                "impuestos", List.of(Map.of(
                        "codigo", "01", "nombre", "IVA", "porcentaje", ivaPct,
                        "es_retencion", false)));
    }

    private void entregar(String tipoEvento, UUID mensajeId, Map<String, Object> payload) {
        try {
            MessageProperties props = new MessageProperties();
            props.setMessageId(mensajeId.toString());
            props.setReceivedRoutingKey(tipoEvento);
            Message mensaje = MessageBuilder.withBody(json.writeValueAsBytes(payload))
                    .andProperties(props).build();
            consumidor.recibir(mensaje);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private FacturaEmitida unicaFactura(UUID negocio) {
        List<FacturaEmitida> lista = enContexto(negocio, admin, VER, () -> facturas.listar());
        assertThat(lista).hasSize(1);
        return lista.get(0);
    }

    @Test
    @DisplayName("Criterio 1: venta_completada emite la factura con origen VENTA")
    void desdeVenta() {
        resolucionEn(negocioA);
        UUID ventaId = UUID.randomUUID();

        entregar("venta_completada", UUID.randomUUID(),
                evento(negocioA, ventaId, "Cliente Uno", List.of(linea("Cemento", "2", "1000", "0", "19"))));

        FacturaEmitida f = unicaFactura(negocioA);
        assertThat(f.origenTipo()).isEqualTo("VENTA");
        assertThat(f.estado()).isEqualTo("GENERADA");
        assertThat(f.numeroCompleto()).isEqualTo("FE1");
        assertThat(comoElServicio(negocioA, "select origen_id::text from facturas where id = '"
                + f.id() + "'")).containsExactly(ventaId.toString());
    }

    @Test
    @DisplayName("Criterio 2: estancia_finalizada emite con origen RESERVA, mismo código")
    void desdeReserva() {
        resolucionEn(negocioA);
        entregar("estancia_finalizada", UUID.randomUUID(),
                evento(negocioA, UUID.randomUUID(), "Huésped", List.of(linea("Noche", "1", "200000", "0", "19"))));

        assertThat(unicaFactura(negocioA).origenTipo()).isEqualTo("RESERVA");
    }

    @Test
    @DisplayName("Criterio 3: pedido_completado emite con origen COMANDA")
    void desdePedido() {
        resolucionEn(negocioA);
        entregar("pedido_completado", UUID.randomUUID(),
                evento(negocioA, UUID.randomUUID(), "Mesa 4", List.of(linea("Menú", "3", "25000", "0", "8"))));

        assertThat(unicaFactura(negocioA).origenTipo()).isEqualTo("COMANDA");
    }

    @Test
    @DisplayName("Criterio 4: el mismo evento entregado dos veces no emite una segunda factura")
    void idempotente() {
        resolucionEn(negocioA);
        UUID ventaId = UUID.randomUUID();
        Map<String, Object> payload = evento(negocioA, ventaId, "Cliente", List.of(linea("X", "1", "1000", "0", "19")));

        UUID mensajeId = UUID.randomUUID();
        entregar("venta_completada", mensajeId, payload);
        entregar("venta_completada", mensajeId, payload);                 // reentrega exacta
        entregar("venta_completada", UUID.randomUUID(), payload);         // otro message-id, mismo origen

        assertThat(enContexto(negocioA, admin, VER, () -> facturas.listar())).hasSize(1);
    }

    @Test
    @DisplayName("Criterios 5 y 6: el emisor y el cliente quedan congelados en la factura")
    void snapshotsCongelados() {
        resolucionEn(negocioA);
        entregar("venta_completada", UUID.randomUUID(),
                evento(negocioA, UUID.randomUUID(), "Cliente Original SAS",
                        List.of(linea("Item", "1", "5000", "0", "19"))));
        UUID facturaId = unicaFactura(negocioA).id();

        FacturaDetalle detalle = enContexto(negocioA, admin, VER, () -> facturas.ver(facturaId));
        assertThat(detalle.cliente()).containsEntry("razon_social", "Cliente Original SAS");
        assertThat(detalle.emisor()).containsEntry("nit", "900111222");

        // Otra factura, con otro cliente: la primera no cambia.
        entregar("venta_completada", UUID.randomUUID(),
                evento(negocioA, UUID.randomUUID(), "Otro Cliente",
                        List.of(linea("Item", "1", "5000", "0", "19"))));
        FacturaDetalle sigueIgual = enContexto(negocioA, admin, VER, () -> facturas.ver(facturaId));
        assertThat(sigueIgual.cliente()).containsEntry("razon_social", "Cliente Original SAS");
    }

    @Test
    @DisplayName("Los totales se recalculan desde las líneas")
    void totalesDesdeLineas() {
        resolucionEn(negocioA);
        entregar("venta_completada", UUID.randomUUID(), evento(negocioA, UUID.randomUUID(),
                "Cliente", List.of(linea("Producto", "2", "1000", "10", "19"))));

        FacturaDetalle d = enContexto(negocioA, admin, VER,
                () -> facturas.ver(unicaFactura(negocioA).id()));
        assertThat(d.subtotal()).isEqualByComparingTo("2000");
        assertThat(d.descuentoTotal()).isEqualByComparingTo("200");
        assertThat(d.baseGravable()).isEqualByComparingTo("1800");
        assertThat(d.impuestosTotal()).isEqualByComparingTo("342");
        assertThat(d.total()).isEqualByComparingTo("2142");
        assertThat(d.lineas()).hasSize(1);
        assertThat(d.impuestos()).hasSize(1);
    }

    @Test
    @DisplayName("Un cierre sin líneas no emite factura")
    void sinLineas() {
        resolucionEn(negocioA);
        assertThatThrownBy(() -> entregar("venta_completada", UUID.randomUUID(),
                evento(negocioA, UUID.randomUUID(), "Cliente", List.of())))
                .hasCauseInstanceOf(com.regenta.comun.errores.ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Las facturas de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        resolucionEn(negocioA);
        entregar("venta_completada", UUID.randomUUID(),
                evento(negocioA, UUID.randomUUID(), "Cliente", List.of(linea("X", "1", "1000", "0", "19"))));

        assertThat(comoElServicio(negocioB, "select count(*) from facturas")).containsExactly("0");
    }
}
