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
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-049. Cuentas por pagar y pagos a proveedores. */
class GestionDeCuentasPorPagarTest extends BaseDeCompras {

    private static final Set<String> COMPRADOR = Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR",
            "COMPRAS_COMPRA_APROBAR", "COMPRAS_PROVEEDOR_VER", "COMPRAS_PROVEEDOR_CREAR",
            "COMPRAS_PROVEEDOR_EDITAR");

    @Autowired
    private GestionDeCuentasPorPagar cuentas;
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

    private UUID proveedor(UUID negocio, String doc, int diasCredito) {
        return enContexto(negocio, usuario, COMPRADOR, () -> proveedores.crear(new SolicitudDeProveedor(
                "NIT", doc, "Proveedor " + doc, null, "C", "p@c.co", "3000000000", "Calle",
                "Bogotá", diasCredito, new BigDecimal("9000000"), 4, null))).id();
    }

    /** Recepción CONFIRMADA sin factura, por `cantidad * costo`. */
    private RecepcionDelNegocio recepcionConfirmada(UUID negocio, UUID prov, String cantidad,
            String costo, String factura) {
        OrdenDelNegocio borrador = enContexto(negocio, usuario, COMPRADOR,
                () -> ordenes.crear(new SolicitudDeOrden(prov, bodega, null, null, BigDecimal.ZERO,
                        null, List.of(new SolicitudDeOrden.LineaDeSolicitud(UUID.randomUUID(),
                                "Producto", new BigDecimal(cantidad), new BigDecimal(costo), null,
                                null)))));
        OrdenDelNegocio aprobada = enContexto(negocio, usuario, COMPRADOR,
                () -> ordenes.aprobar(borrador.id()));
        UUID ol = aprobada.lineas().get(0).id();
        RecepcionDelNegocio rec = enContexto(negocio, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(aprobada.id(), bodega, factura, null,
                        List.of(new SolicitudDeRecepcion.LineaDeRecepcion(ol, new BigDecimal(cantidad),
                                null, false, null, null, null)))));
        return enContexto(negocio, usuario, COMPRADOR, () -> recepciones.confirmar(rec.id(), null));
    }

    @Test
    @DisplayName("Criterio 1: la factura sobre una recepción confirmada abre la cuenta con su vencimiento")
    void facturaAbreCuenta() {
        UUID prov = proveedor(negocioA, "900210021", 45);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "10", "2000", null);

        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-100")));

        assertThat(cuenta.monto()).isEqualByComparingTo("20000");
        assertThat(cuenta.saldo()).isEqualByComparingTo("20000");
        assertThat(cuenta.estado()).isEqualTo("PENDIENTE");
        assertThat(cuenta.fechaVencimiento()).isEqualTo(LocalDate.now().plusDays(45));
        assertThat(comoElServicio(negocioA,
                "select numero_factura from cuentas_por_pagar where id = '" + cuenta.id() + "'"))
                .containsExactly("FP-100");
    }

    @Test
    @DisplayName("Criterio 2: un pago parcial baja el saldo y deja la cuenta en PARCIAL; el resto la salda")
    void pagoParcialYSaldo() {
        UUID prov = proveedor(negocioA, "900220022", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "10", "1000", null);
        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-200")));

        CuentaDelNegocio tras = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarPago(cuenta.id(),
                        new SolicitudDePago(new BigDecimal("4000"), "TRANSFERENCIA", "NEQUI-1")));
        assertThat(tras.saldo()).isEqualByComparingTo("6000");
        assertThat(tras.estado()).isEqualTo("PARCIAL");
        assertThat(tras.pagos()).hasSize(1);

        CuentaDelNegocio saldada = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarPago(cuenta.id(),
                        new SolicitudDePago(new BigDecimal("6000"), "EFECTIVO", null)));
        assertThat(saldada.saldo()).isEqualByComparingTo("0");
        assertThat(saldada.estado()).isEqualTo("PAGADA");
        assertThat(saldada.pagos()).hasSize(2);
    }

    @Test
    @DisplayName("Un pago mayor que el saldo se rechaza")
    void pagoQueSuperaElSaldo() {
        UUID prov = proveedor(negocioA, "900230023", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "5", "1000", null);
        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-300")));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarPago(cuenta.id(),
                        new SolicitudDePago(new BigDecimal("6000"), "EFECTIVO", null))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 3: el listado marca las vencidas y va por antigüedad")
    void listadoMarcaVencidasYOrdena() {
        UUID prov = proveedor(negocioA, "900240024", 30);
        RecepcionDelNegocio r1 = recepcionConfirmada(negocioA, prov, "2", "1000", null);
        RecepcionDelNegocio r2 = recepcionConfirmada(negocioA, prov, "3", "1000", null);
        CuentaDelNegocio c1 = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(r1.id(), "FP-401")));
        CuentaDelNegocio c2 = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(r2.id(), "FP-402")));

        // c1 quedó vencida hace 10 días; c2 vence en el futuro.
        ejecutarComoElServicio(negocioA, "update cuentas_por_pagar set fecha_vencimiento = "
                + "current_date - 10 where id = '" + c1.id() + "'");

        List<CuentaDelNegocio> lista = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.listar(false));

        assertThat(lista).extracting(CuentaDelNegocio::id).containsExactly(c1.id(), c2.id());
        assertThat(lista.get(0).vencida()).isTrue();
        assertThat(lista.get(0).diasDeMora()).isEqualTo(10L);
        assertThat(lista.get(1).vencida()).isFalse();

        List<CuentaDelNegocio> soloVencidas = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.listar(true));
        assertThat(soloVencidas).extracting(CuentaDelNegocio::id).containsExactly(c1.id());
    }

    @Test
    @DisplayName("Criterio 4: una factura repetida para el mismo proveedor responde 409")
    void facturaRepetida() {
        UUID prov = proveedor(negocioA, "900250025", 30);
        RecepcionDelNegocio r1 = recepcionConfirmada(negocioA, prov, "2", "1000", null);
        RecepcionDelNegocio r2 = recepcionConfirmada(negocioA, prov, "2", "1000", null);
        enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(r1.id(), "F-DUP")));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(r2.id(), "F-DUP"))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Facturar una recepción que ya tiene cuenta (traía factura al confirmar) responde 409")
    void recepcionYaFacturada() {
        UUID prov = proveedor(negocioA, "900260026", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "4", "1000", "FP-YA");

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "OTRA"))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("No se factura una recepción que no está confirmada")
    void recepcionNoConfirmada() {
        UUID prov = proveedor(negocioA, "900270027", 30);
        OrdenDelNegocio borrador = enContexto(negocioA, usuario, COMPRADOR,
                () -> ordenes.crear(new SolicitudDeOrden(prov, bodega, null, null, BigDecimal.ZERO,
                        null, List.of(new SolicitudDeOrden.LineaDeSolicitud(UUID.randomUUID(), "P",
                                new BigDecimal("2"), new BigDecimal("1000"), null, null)))));
        OrdenDelNegocio aprobada = enContexto(negocioA, usuario, COMPRADOR,
                () -> ordenes.aprobar(borrador.id()));
        UUID ol = aprobada.lineas().get(0).id();
        RecepcionDelNegocio borradorRec = enContexto(negocioA, usuario, COMPRADOR,
                () -> recepciones.crear(new SolicitudDeRecepcion(aprobada.id(), bodega, null, null,
                        List.of(new SolicitudDeRecepcion.LineaDeRecepcion(ol, new BigDecimal("2"),
                                null, false, null, null, null)))));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(
                        new SolicitudDeFacturaDeRecepcion(borradorRec.id(), "F-X"))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Un método de pago desconocido se rechaza")
    void metodoInvalido() {
        UUID prov = proveedor(negocioA, "900280028", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "2", "1000", null);
        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-500")));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarPago(cuenta.id(),
                        new SolicitudDePago(new BigDecimal("100"), "BITCOIN", null))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Las cuentas y pagos de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        UUID prov = proveedor(negocioA, "900290029", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "5", "1000", null);
        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-600")));
        enContexto(negocioA, usuario, COMPRADOR, () -> cuentas.registrarPago(cuenta.id(),
                new SolicitudDePago(new BigDecimal("1000"), "EFECTIVO", null)));

        assertThat(comoElServicio(negocioB, "select count(*) from cuentas_por_pagar"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB, "select count(*) from pagos_proveedor"))
                .containsExactly("0");
        assertThat(enContexto(negocioB, usuario, COMPRADOR, () -> cuentas.listar(false))).isEmpty();
    }

    @Test
    @DisplayName("Pagar una cuenta ya saldada responde 409")
    void pagarCuentaSaldada() {
        UUID prov = proveedor(negocioA, "900300030", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "2", "1000", null);
        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-700")));
        enContexto(negocioA, usuario, COMPRADOR, () -> cuentas.registrarPago(cuenta.id(),
                new SolicitudDePago(new BigDecimal("2000"), "EFECTIVO", null)));

        assertThatThrownBy(() -> enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarPago(cuenta.id(),
                        new SolicitudDePago(BigDecimal.ONE, "EFECTIVO", null))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("La cuenta se puede consultar por id con sus pagos")
    void verConPagos() {
        UUID prov = proveedor(negocioA, "900310031", 30);
        RecepcionDelNegocio rec = recepcionConfirmada(negocioA, prov, "10", "1000", null);
        CuentaDelNegocio cuenta = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.registrarFactura(new SolicitudDeFacturaDeRecepcion(rec.id(), "FP-800")));
        enContexto(negocioA, usuario, COMPRADOR, () -> cuentas.registrarPago(cuenta.id(),
                new SolicitudDePago(new BigDecimal("2500"), "CHEQUE", "CH-1")));

        CuentaDelNegocio vista = enContexto(negocioA, usuario, COMPRADOR,
                () -> cuentas.ver(cuenta.id()));
        assertThat(vista.saldo()).isEqualByComparingTo("7500");
        assertThat(vista.pagos()).singleElement()
                .satisfies(p -> assertThat(p.metodo()).isEqualTo("CHEQUE"));
    }
}
