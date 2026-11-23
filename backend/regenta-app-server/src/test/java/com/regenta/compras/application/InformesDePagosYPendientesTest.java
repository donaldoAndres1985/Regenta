package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.compras.application.InformesDePagosYPendientes.AntiguedadDeProveedor;
import com.regenta.compras.application.InformesDePagosYPendientes.FaltanteDeOrden;
import com.regenta.compras.application.InformesDePagosYPendientes.InformeDeAntiguedad;
import com.regenta.compras.application.InformesDePagosYPendientes.InformeDePagos;
import com.regenta.compras.application.InformesDePagosYPendientes.PagosPorProveedorYMetodo;
import com.regenta.compras.application.InformesDePagosYPendientes.PendientePorRecibir;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.shared.archivos.ExportadorDeReportes;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/**
 * HU-184 · Cuentas por pagar por antigüedad, pendientes por recibir, faltantes de órdenes cerradas y
 * pagos por proveedor y método. Dos proveedores: Distribuidora Andina y Pinturas del Valle.
 */
class InformesDePagosYPendientesTest {

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final LocalDate hoy = LocalDate.now(InformesDeCompras.ZONA);

    private final CuentaPorPagarRepositorio cuentasRepo = mock(CuentaPorPagarRepositorio.class);
    private final OrdenDeCompraRepositorio ordenesRepo = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio lineasRepo = mock(OrdenCompraLineaRepositorio.class);
    private final PagoProveedorRepositorio pagosRepo = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedoresRepo = mock(ProveedorRepositorio.class);

    private final List<CuentaPorPagar> cuentas = new ArrayList<>();
    private final List<OrdenDeCompra> ordenes = new ArrayList<>();
    private final List<OrdenCompraLinea> lineas = new ArrayList<>();
    private final List<PagoProveedor> pagos = new ArrayList<>();

    private InformesDePagosYPendientes informes;
    private Proveedor andina;
    private Proveedor valle;

    @BeforeEach
    void preparar() {
        conPermisos("COMPRAS_INFORME_VER");
        informes = new InformesDePagosYPendientes(cuentasRepo, ordenesRepo, lineasRepo, pagosRepo, proveedoresRepo,
                new ExportadorDeReportes());
        andina = proveedor("900123456", "Distribuidora Andina");
        valle = proveedor("900765432", "Pinturas del Valle");
        when(proveedoresRepo.findAllById(any())).thenReturn(List.of(andina, valle));
        when(cuentasRepo.findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId)).thenReturn(cuentas);
        when(ordenesRepo.findByNegocioIdAndEstadoIn(eq(negocioId), anyCollection())).thenAnswer(inv -> {
            Collection<EstadoOrdenCompra> estados = inv.getArgument(1);
            return ordenes.stream().filter(o -> estados.contains(o.getEstado())).toList();
        });
        when(lineasRepo.findByOrdenIdInOrderByLinea(anyCollection())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            return lineas.stream().filter(l -> ids.contains(l.getOrdenId())).toList();
        });
        when(pagosRepo.findByNegocioIdAndAnuladoEnIsNullAndFechaBetween(eq(negocioId), any(), any()))
                .thenAnswer(inv -> {
                    LocalDate desde = inv.getArgument(1);
                    LocalDate hasta = inv.getArgument(2);
                    return pagos.stream().filter(p -> !p.getFecha().isBefore(desde) && !p.getFecha().isAfter(hasta))
                            .toList();
                });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(permisos), Set.of()));
    }

    private Proveedor proveedor(String nit, String nombre) {
        return Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, nit, nombre + " SAS", nombre, null, null,
                null, null, null, 30, BigDecimal.ZERO, null, null);
    }

    /** Una cuenta a 30 días que vence hace {@code diasDeMora} días (negativo: aún no vence). */
    private CuentaPorPagar cuenta(Proveedor p, int monto, int diasDeMora) {
        CuentaPorPagar c = CuentaPorPagar.abrirPorRecepcion(negocioId, p.getId(), UUID.randomUUID(),
                "FV-" + (cuentas.size() + 1), BigDecimal.valueOf(monto), hoy.minusDays(30L + diasDeMora), 30);
        cuentas.add(c);
        return c;
    }

    private OrdenDeCompra orden(Proveedor p, String numero, LocalDate esperada) {
        OrdenDeCompra o = OrdenDeCompra.crear(negocioId, numero, p.getId(), UUID.randomUUID(), null, usuarioId,
                esperada, BigDecimal.ZERO, null);
        ordenes.add(o);
        return o;
    }

    private OrdenCompraLinea linea(OrdenDeCompra o, int n, String producto, int pedida, int costo, int descuento) {
        OrdenCompraLinea l = OrdenCompraLinea.nueva(o.getId(), negocioId, n, UUID.randomUUID(), producto,
                BigDecimal.valueOf(pedida), BigDecimal.valueOf(costo), BigDecimal.valueOf(descuento),
                new BigDecimal("19"));
        lineas.add(l);
        return l;
    }

    private void recibir(OrdenDeCompra o, OrdenCompraLinea l, int cantidad) {
        l.recibir(BigDecimal.valueOf(cantidad), BigDecimal.ZERO);
        o.registrarAvanceDeRecepcion(lineas.stream().filter(x -> x.getOrdenId().equals(o.getId())).toList());
    }

    private PagoProveedor pago(Proveedor p, int monto, MetodoDePago metodo) {
        PagoProveedor pago = PagoProveedor.registrar(negocioId, p.getId(), null, BigDecimal.valueOf(monto), metodo,
                null, usuarioId, null);
        pagos.add(pago);
        return pago;
    }

    @Test
    @DisplayName("C1 · Cada proveedor con su saldo en al día, 1-30, 31-60, 61-90 y más de 90, y el total por columna")
    void antiguedad() {
        cuenta(andina, 248000, -20);
        cuenta(andina, 119000, 15).registrarPago(new BigDecimal("19000"));
        cuenta(andina, 50000, 100);
        cuenta(andina, 30000, 40).registrarPago(new BigDecimal("30000"));
        cuenta(valle, 80000, 45);
        cuenta(valle, 60000, 70);
        cuenta(valle, 10000, 30);
        cuenta(valle, 5000, 31);
        cuenta(valle, 70000, 200).anularPorRecepcion();
        cuenta(valle, 1000, 0);

        InformeDeAntiguedad a = informes.antiguedad();

        assertThat(a.fecha()).isEqualTo(hoy);
        assertThat(a.proveedores()).extracting(AntiguedadDeProveedor::proveedor)
                .containsExactly("Distribuidora Andina", "Pinturas del Valle");
        AntiguedadDeProveedor deAndina = a.proveedores().get(0);
        assertThat(deAndina.cuentas()).isEqualTo(3);
        assertThat(deAndina.moraMaxima()).isEqualTo(100);
        assertThat(deAndina.saldo().alDia()).isEqualByComparingTo("248000");
        assertThat(deAndina.saldo().de1a30()).isEqualByComparingTo("100000");
        assertThat(deAndina.saldo().masDe90()).isEqualByComparingTo("50000");
        assertThat(deAndina.saldo().total()).isEqualByComparingTo("398000");
        AntiguedadDeProveedor deValle = a.proveedores().get(1);
        // Vence hoy: al día. 30 días de mora: 1-30. 31: 31-60. La anulada no cuenta.
        assertThat(deValle.saldo().alDia()).isEqualByComparingTo("1000");
        assertThat(deValle.saldo().de1a30()).isEqualByComparingTo("10000");
        assertThat(deValle.saldo().de31a60()).isEqualByComparingTo("85000");
        assertThat(deValle.saldo().de61a90()).isEqualByComparingTo("60000");
        assertThat(deValle.saldo().total()).isEqualByComparingTo("156000");

        assertThat(a.totales().alDia()).isEqualByComparingTo("249000");
        assertThat(a.totales().de1a30()).isEqualByComparingTo("110000");
        assertThat(a.totales().de31a60()).isEqualByComparingTo("85000");
        assertThat(a.totales().de61a90()).isEqualByComparingTo("60000");
        assertThat(a.totales().masDe90()).isEqualByComparingTo("50000");
        assertThat(a.totales().total()).isEqualByComparingTo("554000");
    }

    @Test
    @DisplayName("C2 · Por orden y línea lo que falta y la fecha esperada; las atrasadas primero y marcadas")
    void pendientes() {
        OrdenDeCompra alDia = orden(valle, "OC-2", hoy.plusDays(5));
        OrdenCompraLinea completa = linea(alDia, 1, "Vinilo blanco", 10, 30000, 0);
        OrdenCompraLinea aMedias = linea(alDia, 2, "Brocha 3\"", 5, 20000, 10);
        alDia.confirmar(usuarioId, true, false);
        alDia.enviar();
        recibir(alDia, completa, 10);
        recibir(alDia, aMedias, 2);

        OrdenDeCompra atrasada = orden(andina, "OC-1", hoy.minusDays(3));
        linea(atrasada, 1, "Acetaminofén 500 mg x100", 120, 8400, 0);
        atrasada.confirmar(usuarioId, true, false);
        atrasada.enviar();

        OrdenDeCompra sinFecha = orden(andina, "OC-3", null);
        linea(sinFecha, 1, "Ibuprofeno 400 mg", 4, 20000, 0);
        sinFecha.confirmar(usuarioId, true, false);

        OrdenDeCompra borrador = orden(andina, "OC-4", hoy.minusDays(30));
        linea(borrador, 1, "Amoxicilina", 1, 1000, 0);
        OrdenDeCompra recibida = orden(andina, "OC-5", hoy.minusDays(30));
        OrdenCompraLinea toda = linea(recibida, 1, "Loratadina", 2, 1000, 0);
        recibida.confirmar(usuarioId, true, false);
        recibir(recibida, toda, 2);

        List<PendientePorRecibir> p = informes.pendientesPorRecibir();

        assertThat(p).extracting(PendientePorRecibir::numero).containsExactly("OC-1", "OC-2", "OC-3");
        PendientePorRecibir primera = p.get(0);
        assertThat(primera.atrasada()).isTrue();
        assertThat(primera.diasDeAtraso()).isEqualTo(3);
        assertThat(primera.fechaEsperada()).isEqualTo(hoy.minusDays(3));
        assertThat(primera.proveedor()).isEqualTo("Distribuidora Andina");
        assertThat(primera.lineas().get(0).faltante()).isEqualByComparingTo("120");
        assertThat(primera.valorPendiente()).isEqualByComparingTo("1008000");

        PendientePorRecibir parcial = p.get(1);
        assertThat(parcial.atrasada()).isFalse();
        assertThat(parcial.estado()).isEqualTo(EstadoOrdenCompra.PARCIAL);
        assertThat(parcial.lineas()).singleElement().satisfies(l -> {
            assertThat(l.producto()).isEqualTo("Brocha 3\"");
            assertThat(l.pedida()).isEqualByComparingTo("5");
            assertThat(l.recibida()).isEqualByComparingTo("2");
            assertThat(l.faltante().toPlainString()).isEqualTo("3");
            assertThat(l.valor()).isEqualByComparingTo("54000");
        });
        assertThat(p.get(2).fechaEsperada()).isNull();
        assertThat(p.get(2).atrasada()).isFalse();
    }

    @Test
    @DisplayName("C3 · De las órdenes cerradas con faltante aparece lo que nunca llegó")
    void faltantes() {
        OrdenDeCompra cerrada = orden(andina, "OC-6", hoy.minusDays(10));
        OrdenCompraLinea a = linea(cerrada, 1, "Acetaminofén", 10, 10000, 0);
        OrdenCompraLinea b = linea(cerrada, 2, "Ibuprofeno", 5, 20000, 0);
        cerrada.confirmar(usuarioId, true, false);
        recibir(cerrada, a, 7);
        recibir(cerrada, b, 5);
        cerrada.cerrar(usuarioId, "El proveedor no tenía más");

        OrdenDeCompra abierta = orden(valle, "OC-7", hoy);
        linea(abierta, 1, "Vinilo", 3, 30000, 0);
        abierta.confirmar(usuarioId, true, false);

        List<FaltanteDeOrden> f = informes.faltantes(null, null);

        assertThat(f).singleElement().satisfies(o -> {
            assertThat(o.numero()).isEqualTo("OC-6");
            assertThat(o.motivo()).isEqualTo("El proveedor no tenía más");
            assertThat(o.cerradaEn()).isNotNull();
            assertThat(o.lineas()).singleElement().satisfies(l -> {
                assertThat(l.producto()).isEqualTo("Acetaminofén");
                assertThat(l.faltante()).isEqualByComparingTo("3");
            });
            assertThat(o.valorFaltante()).isEqualByComparingTo("30000");
        });
        assertThat(informes.faltantes(hoy.minusDays(40), hoy.minusDays(1))).isEmpty();
        assertThat(informes.faltantes(hoy, hoy)).hasSize(1);
    }

    @Test
    @DisplayName("C4 · Pagos del periodo por proveedor y método, sin los anulados")
    void pagos() {
        pago(andina, 100000, MetodoDePago.TRANSFERENCIA);
        pago(andina, 50000, MetodoDePago.TRANSFERENCIA);
        pago(andina, 20000, MetodoDePago.EFECTIVO);
        pago(valle, 80000, MetodoDePago.CHEQUE);
        pago(valle, 999, MetodoDePago.EFECTIVO).anular(usuarioId, "Se registró por error", null);

        InformeDePagos p = informes.pagos(hoy, hoy);

        assertThat(p.filas()).extracting(PagosPorProveedorYMetodo::proveedor, PagosPorProveedorYMetodo::metodo,
                PagosPorProveedorYMetodo::pagos).containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Distribuidora Andina", MetodoDePago.EFECTIVO, 1),
                        org.assertj.core.groups.Tuple.tuple("Distribuidora Andina", MetodoDePago.TRANSFERENCIA, 2),
                        org.assertj.core.groups.Tuple.tuple("Pinturas del Valle", MetodoDePago.CHEQUE, 1));
        assertThat(p.filas().get(1).total()).isEqualByComparingTo("150000");
        assertThat(p.porMetodo().get(MetodoDePago.TRANSFERENCIA)).isEqualByComparingTo("150000");
        assertThat(p.porMetodo().get(MetodoDePago.EFECTIVO)).isEqualByComparingTo("20000");
        assertThat(p.porMetodo().get(MetodoDePago.CHEQUE)).isEqualByComparingTo("80000");
        assertThat(p.total()).isEqualByComparingTo("250000");

        assertThat(informes.pagos(hoy.minusDays(30), hoy.minusDays(1)).filas()).isEmpty();
        assertThatThrownBy(() -> informes.pagos(hoy, hoy.minusDays(1))).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Los cuatro se descargan en PDF y en Excel")
    void descargas() {
        cuenta(andina, 248000, 10);
        pago(andina, 100000, MetodoDePago.TRANSFERENCIA);
        for (String informe : InformesDePagosYPendientes.INFORMES) {
            var pdf = informes.archivo(informe, "PDF", hoy, hoy);
            assertThat(new String(pdf.contenido(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
            var xlsx = informes.archivo(informe, "xlsx", hoy, hoy);
            assertThat(new String(xlsx.contenido(), 0, 2, StandardCharsets.ISO_8859_1)).isEqualTo("PK");
            assertThat(xlsx.nombre()).endsWith(".xlsx");
        }
        assertThatThrownBy(() -> informes.archivo("antiguedad", "CSV", null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Sin COMPRAS_INFORME_VER, 403")
    void permiso() {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(informes);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        InformesDePagosYPendientes conGuardia = fabrica.getProxy();

        conPermisos("COMPRAS_CXP_VER");
        assertThatThrownBy(conGuardia::antiguedad).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(conGuardia::pendientesPorRecibir).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.faltantes(null, null)).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.pagos(null, null)).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.archivo("pagos", "PDF", null, null))
                .isInstanceOf(SinPermisoException.class);

        conPermisos("COMPRAS_INFORME_VER");
        assertThat(conGuardia.antiguedad().proveedores()).isEmpty();
    }
}
