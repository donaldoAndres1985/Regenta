package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.caja.application.ConsultaDeCierres.CierreEnBusqueda;
import com.regenta.caja.application.ConsultaDeCierres.MovimientoEnCierre;
import com.regenta.caja.application.ConsultaDeCierres.MovimientosDeCierre;
import com.regenta.caja.application.ConsultaDeCierres.PaginaDeCierres;
import com.regenta.caja.application.ConsultaDeCierres.ResumenDeCierre;
import com.regenta.caja.application.InformesDeCajaEnPdf.InformeEnPdf;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.SinPermisoException;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;
import com.regenta.usuarios.api.ConfiguracionNegocioApi.DatosNegocioTicket;

/**
 * HU-144 · Los informes de caja en PDF. Sale del turno de referencia de HU-143: base $ 100.000,
 * ventas de contado $ 50.000 y a crédito $ 50.000, ingreso $ 10.000, retiro $ 40.000; faltaron
 * $ 2.000 de efectivo.
 */
class InformesDeCajaEnPdfTest {

    private final UUID negocioId = UUID.randomUUID();
    private final UUID sesionId = UUID.randomUUID();
    private final OffsetDateTime cierre = OffsetDateTime.parse("2026-11-20T18:30:00-05:00");

    private final ConsultaDeCierres consulta = mock(ConsultaDeCierres.class);
    private final ConfiguracionNegocioApi config = mock(ConfiguracionNegocioApi.class);
    private InformesDeCajaEnPdf informes;

    @BeforeEach
    void preparar() {
        conPermisos("CAJA_TURNO_VER");
        informes = new InformesDeCajaEnPdf(consulta, config);
        when(config.datosTicket(negocioId)).thenReturn(new DatosNegocioTicket("Ferretería Central SAS",
                "Ferretería Central", "900123456-7", null, null, null, "Medellín"));
        when(consulta.resumen(sesionId)).thenReturn(resumen());
        when(consulta.movimientosDe(sesionId)).thenReturn(movimientos(60));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("CAJA"), Set.of(permisos), Set.of()));
    }

    private CierreEnBusqueda fila(String numero, String esperado, String diferencia) {
        return new CierreEnBusqueda(UUID.randomUUID(), cierre, UUID.randomUUID(), "Caja Principal", numero,
                UUID.randomUUID(), "Ana Ruiz", new BigDecimal(esperado), new BigDecimal(diferencia),
                new BigDecimal(diferencia).signum() == 0 ? "CUADRADA" : "DESCUADRADA");
    }

    private ResumenDeCierre resumen() {
        ArqueoPorMetodo arqueo = new ArqueoPorMetodo(List.of(
                new ArqueoPorMetodo.Linea("EFECTIVO", true, new BigDecimal("100000"), new BigDecimal("98000"),
                        new BigDecimal("-2000"), 6),
                new ArqueoPorMetodo.Linea("TARJETA_DEBITO", true, new BigDecimal("20000"), new BigDecimal("20000"),
                        BigDecimal.ZERO, 1),
                new ArqueoPorMetodo.Linea("CREDITO", false, new BigDecimal("50000"), new BigDecimal("50000"),
                        BigDecimal.ZERO, 1)));
        return new ResumenDeCierre(new CierreEnBusqueda(sesionId, cierre, UUID.randomUUID(), "Caja Principal", "CAJA-12",
                UUID.randomUUID(), "Ana Ruiz", new BigDecimal("170000"), new BigDecimal("-2000"), "DESCUADRADA"),
                "Luis Gómez", cierre.minusHours(10), new BigDecimal("100000"), new BigDecimal("50000"),
                new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000"), new BigDecimal("40000"),
                BigDecimal.ZERO, BigDecimal.ZERO, arqueo, "Faltó sencillo");
    }

    /** La venta V-1, el retiro y {@code ventas} ventas más, para que el detallado pase de una página. */
    private MovimientosDeCierre movimientos(int ventas) {
        List<MovimientoEnCierre> m = new ArrayList<>();
        m.add(new MovimientoEnCierre(UUID.randomUUID(), cierre.minusHours(9), "VENTA", "Venta", "V-1", "EFECTIVO",
                "Efectivo", new BigDecimal("30000"), false));
        m.add(new MovimientoEnCierre(UUID.randomUUID(), cierre.minusHours(8), "RETIRO", "Retiro", null, "EFECTIVO",
                "Efectivo", new BigDecimal("-40000"), false));
        m.add(new MovimientoEnCierre(UUID.randomUUID(), cierre.minusHours(7), "GASTO", "Gasto", null, "EFECTIVO",
                "Efectivo", new BigDecimal("-5000"), true));
        for (int i = 0; i < ventas; i++) {
            m.add(new MovimientoEnCierre(UUID.randomUUID(), cierre.minusHours(6), "VENTA", "Venta", "V-" + (100 + i),
                    "TARJETA_DEBITO", "Tarjeta débito", new BigDecimal("1000"), false));
        }
        return new MovimientosDeCierre(sesionId, m, new BigDecimal("45000"));
    }

    private static String texto(InformeEnPdf informe) throws Exception {
        assertThat(new String(informe.contenido(), 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        try (PDDocument pdf = Loader.loadPDF(informe.contenido())) {
            return new PDFTextStripper().getText(pdf);
        }
    }

    private static int paginas(InformeEnPdf informe) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(informe.contenido())) {
            return pdf.getNumberOfPages();
        }
    }

    @Test
    @DisplayName("C1 · El informe general trae el resumen y el arqueo por método, sin los movimientos")
    void general() throws Exception {
        InformeEnPdf pdf = informes.deCierre(sesionId, false);
        String t = texto(pdf);

        assertThat(pdf.nombre()).isEqualTo("cierre-caja-12-general.pdf");
        assertThat(t).contains("Ferretería Central", "NIT 900123456-7", "INFORME GENERAL DE CAJA CAJA-12",
                "Caja Principal · DESCUADRADA", "Abrió Ana Ruiz el 20/11/2026 08:30",
                "Cerró Luis Gómez el 20/11/2026 18:30");
        assertThat(t).contains("Base de apertura", "$ 100.000", "Ventas de contado", "$ 50.000", "Ventas a crédito",
                "Ingresos", "$ 10.000", "Retiros", "-$ 40.000", "Gastos", "$ 0");
        assertThat(t).doesNotContain("-$ 0");
        assertThat(t).contains("Arqueo por método", "Método", "Efectivo", "$ 98.000", "-$ 2.000", "Tarjeta débito", "Crédito",
                "Total", "$ 170.000", "$ 168.000");
        assertThat(t).contains("Faltó sencillo");
        assertThat(t).doesNotContain("Movimientos", "V-1");
        verify(consulta, never()).movimientosDe(any());
    }

    @Test
    @DisplayName("C2 · El detallado trae además todos los movimientos con el total, en las páginas que hagan falta")
    void detallado() throws Exception {
        InformeEnPdf pdf = informes.deCierre(sesionId, true);
        String t = texto(pdf);

        assertThat(pdf.nombre()).isEqualTo("cierre-caja-12-detallado.pdf");
        assertThat(t).contains("INFORME DETALLADO DE CAJA CAJA-12", "Arqueo por método", "Movimientos");
        assertThat(t).contains("20/11/2026 09:30", "Venta", "V-1", "-$ 40.000", "Gasto (anulado)", "V-159",
                "$ 45.000");
        assertThat(paginas(pdf)).isGreaterThan(1);
        // El encabezado de la tabla se repite en la página siguiente.
        try (PDDocument doc = Loader.loadPDF(pdf.contenido())) {
            PDFTextStripper ultima = new PDFTextStripper();
            ultima.setStartPage(doc.getNumberOfPages());
            ultima.setEndPage(doc.getNumberOfPages());
            assertThat(ultima.getText(doc)).contains("Documento", "Monto", "Total");
        }
    }

    @Test
    @DisplayName("C3 · Los cierres de un rango, uno por fila, con los totales; trae todas las páginas")
    void rango() throws Exception {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 23);
        when(consulta.buscar(desde, hasta, null, 0, 100)).thenReturn(new PaginaDeCierres(
                List.of(fila("CAJA-12", "170000", "-2000"), fila("CAJA-11", "250000", "0")), 0, 100, 3, true));
        when(consulta.buscar(desde, hasta, null, 1, 100))
                .thenReturn(new PaginaDeCierres(List.of(fila("CAJA-10", "80000", "500")), 1, 100, 3, false));

        InformeEnPdf pdf = informes.deCierres(desde, hasta, null);
        String t = texto(pdf);

        assertThat(pdf.nombre()).isEqualTo("cierres-de-caja-2026-11-01-a-2026-11-23.pdf");
        assertThat(t).contains("CIERRES DE CAJA DEL 01/11/2026 AL 23/11/2026");
        assertThat(t).contains("CAJA-12", "CAJA-11", "CAJA-10", "20/11/2026 18:30", "Ana Ruiz", "$ 250.000",
                "DESCUADRADA", "CUADRADA");
        assertThat(t).contains("Total del rango", "3 cierres", "2 con diferencia", "$ 500.000", "-$ 1.500");
    }

    @Test
    @DisplayName("C3 · Un rango sin cierres también sale, y dice que no hubo")
    void rangoVacio() throws Exception {
        LocalDate dia = LocalDate.of(2026, 11, 23);
        when(consulta.buscar(eq(dia), eq(dia), any(), anyInt(), anyInt()))
                .thenReturn(new PaginaDeCierres(List.of(), 0, 100, 0, false));

        String t = texto(informes.deCierres(dia, dia, UUID.randomUUID()));

        assertThat(t).contains("0 cierres", "No hubo cierres de caja en este rango.");
    }

    @Test
    @DisplayName("C4 · El negocio B no saca el informe de un cierre del negocio A: sale de la consulta del negocio actual")
    void otroNegocio() {
        UUID deOtro = UUID.randomUUID();
        when(consulta.resumen(deOtro)).thenThrow(new NoEncontradoException("Ese cierre de caja no existe"));

        assertThatThrownBy(() -> informes.deCierre(deOtro, true)).isInstanceOf(NoEncontradoException.class);
        verify(consulta, never()).movimientosDe(deOtro);
    }

    @Test
    @DisplayName("Sin CAJA_TURNO_VER, 403")
    void permiso() {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(informes);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        InformesDeCajaEnPdf conGuardia = fabrica.getProxy();
        conPermisos("CAJA_MOVIMIENTO_REGISTRAR");

        assertThatThrownBy(() -> conGuardia.deCierre(sesionId, false)).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.deCierres(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), null))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Los montos en pesos colombianos, sin decimales")
    void pesos() {
        assertThat(InformesDeCajaEnPdf.pesos(new BigDecimal("1250000.4"))).isEqualTo("$ 1.250.000");
        assertThat(InformesDeCajaEnPdf.pesos(new BigDecimal("-2000"))).isEqualTo("-$ 2.000");
        assertThat(InformesDeCajaEnPdf.pesos(new BigDecimal("-0.4"))).isEqualTo("$ 0");
    }
}
