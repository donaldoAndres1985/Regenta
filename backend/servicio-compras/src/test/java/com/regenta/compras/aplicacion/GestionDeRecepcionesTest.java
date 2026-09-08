package com.regenta.compras.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.compras.BaseDeCompras;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-048. Recepción de mercancía con captura de lotes. */
class GestionDeRecepcionesTest extends BaseDeCompras {

    private static final Set<String> COMPRADOR = Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR",
            "COMPRAS_COMPRA_APROBAR", "COMPRAS_PROVEEDOR_VER", "COMPRAS_PROVEEDOR_CREAR",
            "COMPRAS_PROVEEDOR_EDITAR");

    @Autowired
    private GestionDeRecepciones recepciones;
    @Autowired
    private GestionDeOrdenesDeCompra ordenes;
    @Autowired
    private GestionDeProveedores proveedores;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID proveedorConPlazo(UUID negocio, String doc, int diasCredito) {
        return enContexto(negocio, usuario, COMPRADOR, () -> proveedores.crear(new SolicitudDeProveedor(
                "NIT", doc, "Proveedor " + doc, null, "Contacto", "p@correo.co", "3000000000",
                "Calle", "Bogotá", diasCredito, new BigDecimal("9000000"), 4, null))).id();
    }

    /** Orden aprobada con una línea; devuelve (ordenId, ordenLineaId, cantidadPedida). */
    private OrdenDelNegocio ordenAprobada(UUID negocio, UUID proveedorId, String cantidad,
            String costo) {
        OrdenDelNegocio borrador = enContexto(negocio, usuario, COMPRADOR,
                () -> ordenes.crear(new SolicitudDeOrden(proveedorId, bodega, null, null,
                        BigDecimal.ZERO, null,
                        List.of(new SolicitudDeOrden.LineaDeSolicitud(UUID.randomUUID(), "Producto",
                                new BigDecimal(cantidad), new BigDecimal(costo), null, null)))));
        return enContexto(negocio, usuario, COMPRADOR, () -> ordenes.aprobar(borrador.id()));
    }

    private SolicitudDeRecepcion.LineaDeRecepcion recibir(UUID ordenLineaId, String cantidad,
            boolean exigeLote, String lote, LocalDate vence) {
        return new SolicitudDeRecepcion.LineaDeRecepcion(ordenLineaId, new BigDecimal(cantidad),
                null, exigeLote, lote, vence, null);
    }

    @Test
    @DisplayName("Criterio 1: recibir un producto que exige lote sin capturarlo responde 422")
    void faltaElLote() {
        UUID prov = proveedorConPlazo(negocioA, "900110011", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "5", true, null, null))))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 2: un producto que no maneja lotes no guarda datos de lote")
    void sinLoteNoGuardaLote() {
        UUID prov = proveedorConPlazo(negocioA, "900120012", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(new SolicitudDeRecepcion.LineaDeRecepcion(ol, new BigDecimal("4"),
                                null, false, "LOTE-IGNORADO", LocalDate.now().plusYears(1),
                                "RS-123")))));

        assertThat(rec.lineas().get(0).codigoLote()).isNull();
        assertThat(rec.lineas().get(0).fechaVencimiento()).isNull();
        assertThat(comoElServicio(negocioA,
                "select count(*) from recepcion_lineas where codigo_lote is not null"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Criterio 3: al confirmar se publica recepcion_registrada con las líneas y sus lotes")
    void publicaRecepcionRegistrada() {
        UUID prov = proveedorConPlazo(negocioA, "900130013", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();
        LocalDate vence = LocalDate.now().plusMonths(8);

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "10", true, "L-2026-09", vence)))));
        enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.confirmar(rec.id(), null));

        assertThat(comoElServicio(negocioA,
                "select count(*) from outbox_eventos where agregado_id = '" + rec.id()
                        + "' and tipo_evento = 'recepcion_registrada'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select payload::text from outbox_eventos where agregado_id = '" + rec.id() + "'")
                .get(0)).contains("L-2026-09").contains(vence.toString());
    }

    @Test
    @DisplayName("Criterio 4: recibir por encima de lo pedido más el 5% se rechaza (app y base)")
    void superaLaTolerancia() {
        UUID prov = proveedorConPlazo(negocioA, "900140014", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "11", false, null, null)))));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.confirmar(rec.id(), null)))
                .isInstanceOf(ReglaDeNegocioException.class);

        // La garantía también vive en la base: ck_recibida_oc.
        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA,
                "update orden_compra_lineas set cantidad_recibida = cantidad_pedida * 1.10 where id = '"
                        + ol + "'"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 5: una recepción parcial deja la orden en PARCIAL y admite otra")
    void recepcionParcialYLuegoCompleta() {
        UUID prov = proveedorConPlazo(negocioA, "900150015", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio primera = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "6", false, null, null)))));
        enContexto(negocioA, usuario, COMPRADOR, () -> recepciones.confirmar(primera.id(), null));

        assertThat(comoElServicio(negocioA,
                "select estado from ordenes_compra where id = '" + orden.id() + "'"))
                .containsExactly("PARCIAL");

        RecepcionDelNegocio segunda = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "4", false, null, null)))));
        enContexto(negocioA, usuario, COMPRADOR, () -> recepciones.confirmar(segunda.id(), null));

        assertThat(comoElServicio(negocioA,
                "select estado from ordenes_compra where id = '" + orden.id() + "'"))
                .containsExactly("RECIBIDA");
        assertThat(comoElServicio(negocioA,
                "select cantidad_recibida from orden_compra_lineas where id = '" + ol + "'")
                .get(0)).startsWith("10");
    }

    @Test
    @DisplayName("Criterio 6: la cuenta por pagar vence a los días de crédito del proveedor")
    void cuentaPorPagarTomaElPlazo() {
        UUID prov = proveedorConPlazo(negocioA, "900160016", 45);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "2000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, "FAC-9001", null,
                        List.of(recibir(ol, "10", false, null, null)))));
        enContexto(negocioA, usuario, COMPRADOR, () -> recepciones.confirmar(rec.id(), null));

        List<String> venc = comoElServicio(negocioA,
                "select fecha_vencimiento from cuentas_por_pagar where recepcion_id = '" + rec.id() + "'");
        assertThat(venc).containsExactly(LocalDate.now().plusDays(45).toString());
        assertThat(comoElServicio(negocioA,
                "select monto from cuentas_por_pagar where recepcion_id = '" + rec.id() + "'")
                .get(0)).startsWith("20000");
        assertThat(comoElServicio(negocioA,
                "select numero_factura from cuentas_por_pagar where recepcion_id = '" + rec.id() + "'"))
                .containsExactly("FAC-9001");
    }

    @Test
    @DisplayName("Una recepción sin factura no abre cuenta por pagar")
    void sinFacturaNoAbreCuenta() {
        UUID prov = proveedorConPlazo(negocioA, "900161016", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "5", "1000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "5", false, null, null)))));
        enContexto(negocioA, usuario, COMPRADOR, () -> recepciones.confirmar(rec.id(), null));

        assertThat(comoElServicio(negocioA, "select count(*) from cuentas_por_pagar"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Confirmar dos veces la misma recepción responde 409")
    void confirmarDosVeces() {
        UUID prov = proveedorConPlazo(negocioA, "900170017", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "5", false, null, null)))));
        enContexto(negocioA, usuario, COMPRADOR, () -> recepciones.confirmar(rec.id(), null));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.confirmar(rec.id(), null)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("No se recibe mercancía de una orden que sigue en borrador")
    void ordenEnBorradorNoRecibe() {
        UUID prov = proveedorConPlazo(negocioA, "900180018", 30);
        OrdenDelNegocio borrador = enContexto(negocioA, usuario, COMPRADOR,
                () -> ordenes.crear(new SolicitudDeOrden(prov, bodega, null, null, BigDecimal.ZERO,
                        null, List.of(new SolicitudDeOrden.LineaDeSolicitud(UUID.randomUUID(),
                                "Producto", new BigDecimal("3"), new BigDecimal("500"), null, null)))));
        UUID ol = borrador.lineas().get(0).id();

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(borrador.id(), bodega, null, null,
                        List.of(recibir(ol, "3", false, null, null))))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Las recepciones de un negocio no se ven desde otro")
    void aisladaPorNegocio() {
        UUID prov = proveedorConPlazo(negocioA, "900190019", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();
        enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "5", false, null, null)))));

        assertThat(comoElServicio(negocioB, "select count(*) from recepciones"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB, "select count(*) from recepcion_lineas"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Confirmar una recepción parcial sí se puede (no exige recibir todo)")
    void confirmarParcialSePuede() {
        UUID prov = proveedorConPlazo(negocioA, "900200020", 30);
        OrdenDelNegocio orden = ordenAprobada(negocioA, prov, "10", "1000");
        UUID ol = orden.lineas().get(0).id();

        RecepcionDelNegocio rec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(orden.id(), bodega, null, null,
                        List.of(recibir(ol, "2", false, null, null)))));

        assertThatCode(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.confirmar(rec.id(), null))).doesNotThrowAnyException();
    }
}
