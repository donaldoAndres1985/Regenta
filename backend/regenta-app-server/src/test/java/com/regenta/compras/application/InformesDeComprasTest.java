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
import java.time.OffsetDateTime;
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

import com.regenta.compras.application.InformesDeCompras.CompraPorProducto;
import com.regenta.compras.application.InformesDeCompras.CompraPorProveedor;
import com.regenta.compras.application.InformesDeCompras.CostoHistorico;
import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.DevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.LineaDeDevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.archivos.ExportadorDeReportes;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/**
 * HU-183 · Informes de compras y de costos. Factura de referencia, compra directa a Distribuidora
 * Andina: A 10 × $ 10.000 y B 5 × $ 20.000, IVA 19 %, flete $ 10.000 → base $ 200.000, IVA $ 38.000,
 * total $ 248.000 (A entra a $ 10.500 y B a $ 21.000 con el flete repartido). Además, contra orden,
 * A 10 × $ 10.000 sin flete ($ 119.000); una recepción anulada (A 3 × $ 9.000) que no debe sumar, y
 * una devolución de 1 A ($ 11.900).
 */
class InformesDeComprasTest {

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID productoA = UUID.randomUUID();
    private final UUID productoB = UUID.randomUUID();

    private final RecepcionRepositorio recepcionesRepo = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio lineasRepo = mock(RecepcionLineaRepositorio.class);
    private final DevolucionAProveedorRepositorio devolucionesRepo = mock(DevolucionAProveedorRepositorio.class);
    private final LineaDeDevolucionAProveedorRepositorio lineasDevueltasRepo =
            mock(LineaDeDevolucionAProveedorRepositorio.class);
    private final ProveedorRepositorio proveedoresRepo = mock(ProveedorRepositorio.class);

    private final List<Recepcion> todas = new ArrayList<>();
    private final List<RecepcionLinea> todasLasLineas = new ArrayList<>();
    private final List<DevolucionAProveedor> devoluciones = new ArrayList<>();
    private final List<LineaDeDevolucionAProveedor> lineasDevueltas = new ArrayList<>();

    private InformesDeCompras informes;
    private Proveedor andina;
    private Recepcion directa;
    private Recepcion conOrden;

    @BeforeEach
    void preparar() {
        conPermisos("COMPRAS_INFORME_VER");
        informes = new InformesDeCompras(recepcionesRepo, lineasRepo, devolucionesRepo, lineasDevueltasRepo,
                proveedoresRepo, new ExportadorDeReportes());
        andina = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                "Andina", null, null, null, null, null, 30, BigDecimal.ZERO, null, null);

        directa = recibir("REC-1", null, BigDecimal.valueOf(10000), linea(productoA, "Acetaminofén", 10, 10000),
                linea(productoB, "Ibuprofeno", 5, 20000));
        conOrden = recibir("REC-2", UUID.randomUUID(), BigDecimal.ZERO, linea(productoA, "Acetaminofén", 10, 10000));
        Recepcion anulada = recibir("REC-3", null, BigDecimal.ZERO, linea(productoA, "Acetaminofén", 3, 9000));
        anulada.anular(usuarioId, "Se registró dos veces");

        RecepcionLinea a = todasLasLineas.stream()
                .filter(l -> l.getRecepcionId().equals(directa.getId()) && l.getProductoId().equals(productoA))
                .findFirst().orElseThrow();
        DevolucionAProveedor dev = DevolucionAProveedor.registrar(negocioId, "DEV-1", directa, "Llegó roto",
                new BigDecimal("11900"), usuarioId);
        devoluciones.add(dev);
        lineasDevueltas.add(LineaDeDevolucionAProveedor.de(negocioId, dev.getId(), a, BigDecimal.ONE,
                new BigDecimal("10000"), new BigDecimal("1900")));

        when(recepcionesRepo.findByNegocioIdAndEstadoAndFechaGreaterThanEqualAndFechaLessThan(eq(negocioId), any(),
                any(), any())).thenAnswer(inv -> {
                    EstadoRecepcion estado = inv.getArgument(1);
                    OffsetDateTime desde = inv.getArgument(2);
                    OffsetDateTime hasta = inv.getArgument(3);
                    return todas.stream().filter(r -> r.getEstado() == estado && !r.getFecha().isBefore(desde)
                            && r.getFecha().isBefore(hasta)).toList();
                });
        when(recepcionesRepo.findAllById(any())).thenAnswer(inv -> {
            Iterable<UUID> ids = inv.getArgument(0);
            List<UUID> lista = new ArrayList<>();
            ids.forEach(lista::add);
            return todas.stream().filter(r -> lista.contains(r.getId())).toList();
        });
        when(lineasRepo.findByRecepcionIdIn(anyCollection())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            return todasLasLineas.stream().filter(l -> ids.contains(l.getRecepcionId())).toList();
        });
        when(lineasRepo.findByNegocioIdAndProductoId(eq(negocioId), any())).thenAnswer(inv -> {
            UUID producto = inv.getArgument(1);
            return todasLasLineas.stream().filter(l -> l.getProductoId().equals(producto)).toList();
        });
        when(devolucionesRepo.findByNegocioIdAndFechaGreaterThanEqualAndFechaLessThan(eq(negocioId), any(), any()))
                .thenAnswer(inv -> {
                    OffsetDateTime desde = inv.getArgument(1);
                    OffsetDateTime hasta = inv.getArgument(2);
                    return devoluciones.stream()
                            .filter(d -> !d.getFecha().isBefore(desde) && d.getFecha().isBefore(hasta)).toList();
                });
        when(lineasDevueltasRepo.findByDevolucionIdIn(anyCollection())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            return lineasDevueltas.stream().filter(l -> ids.contains(l.getDevolucionId())).toList();
        });
        when(proveedoresRepo.findAllById(any())).thenReturn(List.of(andina));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(permisos), Set.of()));
    }

    private RecepcionLinea.Datos linea(UUID producto, String nombre, int cantidad, int costo) {
        return new RecepcionLinea.Datos(null, producto, nombre, null, BigDecimal.valueOf(cantidad),
                BigDecimal.valueOf(costo), BigDecimal.ZERO, "IVA_19", new BigDecimal("19"), false, false, null, null,
                null);
    }

    private Recepcion recibir(String numero, UUID ordenId, BigDecimal flete, RecepcionLinea.Datos... datos) {
        Recepcion r = Recepcion.crear(negocioId, numero, ordenId, andina.getId(), UUID.randomUUID(), null,
                usuarioId, null);
        r.datosDeFactura("FV-" + numero, LocalDate.now(), flete, BigDecimal.ZERO, null);
        List<RecepcionLinea> ls = new ArrayList<>();
        for (RecepcionLinea.Datos d : datos) {
            ls.add(RecepcionLinea.nueva(r.getId(), negocioId, d, LocalDate.now()));
        }
        r.totalizar(ls, true);
        r.confirmar();
        todas.add(r);
        todasLasLineas.addAll(ls);
        return r;
    }

    private static LocalDate hoy() {
        return LocalDate.now(InformesDeCompras.ZONA);
    }

    @Test
    @DisplayName("C1 · Por proveedor: base, IVA y total separando directas y con orden; anuladas no suman, "
            + "devoluciones restan")
    void porProveedor() {
        List<CompraPorProveedor> filas = informes.porProveedor(hoy(), hoy());

        assertThat(filas).hasSize(1);
        CompraPorProveedor c = filas.get(0);
        assertThat(c.proveedor()).isEqualTo("Andina");
        assertThat(c.recepciones()).isEqualTo(2);
        assertThat(c.directas().base()).isEqualByComparingTo("200000");
        assertThat(c.directas().iva()).isEqualByComparingTo("38000");
        assertThat(c.directas().total()).isEqualByComparingTo("248000");
        assertThat(c.conOrden().base()).isEqualByComparingTo("100000");
        assertThat(c.conOrden().iva()).isEqualByComparingTo("19000");
        assertThat(c.conOrden().total()).isEqualByComparingTo("119000");
        assertThat(c.devoluciones().total()).isEqualByComparingTo("11900");
        assertThat(c.neto().base()).isEqualByComparingTo("290000");
        assertThat(c.neto().iva()).isEqualByComparingTo("55100");
        assertThat(c.neto().total()).isEqualByComparingTo("355100");
    }

    @Test
    @DisplayName("C1 · Fuera del periodo no cuenta nada")
    void fueraDelPeriodo() {
        assertThat(informes.porProveedor(hoy().minusDays(10), hoy().minusDays(1))).isEmpty();
        assertThat(informes.porProducto(hoy().plusDays(1), hoy().plusDays(2))).isEmpty();
    }

    @Test
    @DisplayName("C2 · Por producto: cantidad, costo total y costo de entrada promedio, neto de devoluciones")
    void porProducto() {
        List<CompraPorProducto> filas = informes.porProducto(hoy(), hoy());

        assertThat(filas).extracting(CompraPorProducto::producto).containsExactly("Acetaminofén", "Ibuprofeno");
        CompraPorProducto a = filas.get(0);
        // 10 × 10.500 (con flete) + 10 × 10.000 − 1 × 10.500 devuelto; la anulada no entra.
        assertThat(a.cantidad()).isEqualByComparingTo("19");
        assertThat(a.costoTotal()).isEqualByComparingTo("194500");
        assertThat(a.costoPromedio()).isEqualByComparingTo("10236.84");
        CompraPorProducto b = filas.get(1);
        assertThat(b.cantidad()).isEqualByComparingTo("5");
        assertThat(b.cantidad().toPlainString()).isEqualTo("5");
        assertThat(b.costoTotal()).isEqualByComparingTo("105000");
        assertThat(b.costoPromedio()).isEqualByComparingTo("21000");
    }

    @Test
    @DisplayName("C3 · Historial de costos: cada costo de entrada con fecha, proveedor y recepción")
    void historial() {
        List<CostoHistorico> historial = informes.historialDeCostos(productoA, null, null);

        assertThat(historial).extracting(CostoHistorico::recepcion).containsExactlyInAnyOrder("REC-1", "REC-2");
        CostoHistorico deLaDirecta = historial.stream().filter(h -> h.recepcion().equals("REC-1")).findFirst()
                .orElseThrow();
        assertThat(deLaDirecta.costoEntrada()).isEqualByComparingTo("10500");
        assertThat(deLaDirecta.cantidad().toPlainString()).isEqualTo("10");
        assertThat(deLaDirecta.proveedor()).isEqualTo("Andina");
        assertThat(deLaDirecta.recepcionId()).isEqualTo(directa.getId());
        assertThat(deLaDirecta.conOrden()).isFalse();
        assertThat(deLaDirecta.fecha()).isNotNull();
        CostoHistorico deLaOrden = historial.stream().filter(h -> h.recepcion().equals("REC-2")).findFirst()
                .orElseThrow();
        assertThat(deLaOrden.costoEntrada()).isEqualByComparingTo("10000");
        assertThat(deLaOrden.conOrden()).isTrue();
        assertThat(deLaOrden.recepcionId()).isEqualTo(conOrden.getId());
    }

    @Test
    @DisplayName("C4 · Cualquier informe se descarga en PDF y en Excel")
    void descargas() {
        for (String informe : List.of("por-proveedor", "por-producto", "historial-de-costos")) {
            var pdf = informes.archivo(informe, "pdf", hoy(), hoy(), productoA);
            assertThat(new String(pdf.contenido(), 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
            assertThat(pdf.tipo()).isEqualTo("application/pdf");
            assertThat(pdf.nombre()).endsWith(".pdf");

            var xlsx = informes.archivo(informe, "XLSX", hoy(), hoy(), productoA);
            assertThat(new String(xlsx.contenido(), 0, 2, StandardCharsets.ISO_8859_1)).isEqualTo("PK");
            assertThat(xlsx.nombre()).endsWith(".xlsx");
        }
        assertThatThrownBy(() -> informes.archivo("por-proveedor", "CSV", hoy(), hoy(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> informes.archivo("ventas", "PDF", hoy(), hoy(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> informes.archivo("historial-de-costos", "PDF", hoy(), hoy(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Un periodo al revés es 422; el periodo va por días completos en la hora de Colombia")
    void periodo() {
        assertThatThrownBy(() -> informes.porProveedor(hoy(), hoy().minusDays(1)))
                .isInstanceOf(ReglaDeNegocioException.class);
        OffsetDateTime[] rango = InformesDeCompras.rango(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertThat(rango[0]).isEqualTo(OffsetDateTime.parse("2026-11-01T00:00-05:00"));
        assertThat(rango[1]).isEqualTo(OffsetDateTime.parse("2026-12-01T00:00-05:00"));
    }

    @Test
    @DisplayName("C5 · Sin COMPRAS_INFORME_VER, 403 en todos los informes")
    void permiso() {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(informes);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        InformesDeCompras conGuardia = fabrica.getProxy();

        conPermisos("COMPRAS_COMPRA_VER");
        assertThatThrownBy(() -> conGuardia.porProveedor(hoy(), hoy())).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.porProducto(hoy(), hoy())).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.historialDeCostos(productoA, null, null))
                .isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.archivo("por-proveedor", "PDF", hoy(), hoy(), null))
                .isInstanceOf(SinPermisoException.class);

        conPermisos("COMPRAS_INFORME_VER");
        assertThat(conGuardia.porProveedor(hoy(), hoy())).hasSize(1);
    }
}
