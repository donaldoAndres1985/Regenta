package com.regenta.caja.application;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.application.ConsultaDeCierres.CierreEnBusqueda;
import com.regenta.caja.application.ConsultaDeCierres.MovimientoEnCierre;
import com.regenta.caja.application.ConsultaDeCierres.MovimientosDeCierre;
import com.regenta.caja.application.ConsultaDeCierres.PaginaDeCierres;
import com.regenta.caja.application.ConsultaDeCierres.ResumenDeCierre;
import com.regenta.caja.application.HojaPdf.Columna;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;
import com.regenta.usuarios.api.ConfiguracionNegocioApi.DatosNegocioTicket;

/**
 * HU-144 · Los informes de caja en PDF para archivar o mandárselos al contador (los de Sara:
 * {@code reporteGeneralCaja}, {@code reporteDetalladoCaja} y {@code cierresCaja}).
 *
 * <ul>
 * <li>C1 · General de un cierre: el resumen y el arqueo por método.</li>
 * <li>C2 · Detallado: lo mismo y todos sus movimientos con el total.</li>
 * <li>C3 · Cierres de un rango: uno por fila y los totales del rango.</li>
 * <li>C4 · Los datos salen de {@link ConsultaDeCierres}, que solo ve el negocio actual.</li>
 * </ul>
 */
@Service
public class InformesDeCajaEnPdf {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConsultaDeCierres consulta;
    private final ConfiguracionNegocioApi negocio;

    public InformesDeCajaEnPdf(ConsultaDeCierres consulta,
            @Autowired(required = false) ConfiguracionNegocioApi negocio) {
        this.consulta = consulta;
        this.negocio = negocio;
    }

    /** Un informe listo para descargar. */
    public record InformeEnPdf(String nombre, byte[] contenido) {
    }

    /** C1 / C2 · {@code detallado}: con todos los movimientos. */
    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public InformeEnPdf deCierre(UUID sesionId, boolean detallado) {
        ResumenDeCierre r = consulta.resumen(sesionId);
        MovimientosDeCierre movs = detallado ? consulta.movimientosDe(sesionId) : null;
        CierreEnBusqueda c = r.cierre();
        try (HojaPdf hoja = new HojaPdf(false)) {
            encabezado(hoja);
            hoja.titulo((detallado ? "INFORME DETALLADO DE CAJA " : "INFORME GENERAL DE CAJA ") + c.numero());
            hoja.subtitulo(c.caja() + " · " + c.estado());
            hoja.subtitulo("Abrió " + c.cajero() + (r.abiertaEn() == null ? "" : " el " + fechaHora(r.abiertaEn())));
            hoja.subtitulo("Cerró " + (r.cerradoPor() == null ? "-" : r.cerradoPor())
                    + (c.cerradaEn() == null ? "" : " el " + fechaHora(c.cerradaEn())));

            hoja.seccion("Resumen");
            hoja.par("Base de apertura", pesos(r.montoApertura()), false);
            hoja.par("Ventas de contado", pesos(r.ventasContado()), false);
            hoja.par("Ventas a crédito", pesos(r.ventasCredito()), false);
            if (r.devoluciones().signum() != 0) {
                hoja.par("Devoluciones", menos(r.devoluciones()), false);
            }
            if (r.recaudos().signum() != 0) {
                hoja.par("Recaudos de cartera", pesos(r.recaudos()), false);
            }
            hoja.par("Ingresos", pesos(r.ingresos()), false);
            hoja.par("Retiros", menos(r.retiros()), false);
            hoja.par("Gastos", menos(r.gastos()), false);
            if (r.pagosAProveedor().signum() != 0) {
                hoja.par("Pagos a proveedores", menos(r.pagosAProveedor()), false);
            }

            hoja.seccion("Arqueo por método");
            float a = hoja.ancho();
            hoja.tabla(List.of(new Columna("Método", HojaPdf.MARGEN, 220, false),
                    new Columna("Esperado", 220, 320, true), new Columna("Contado", 320, 420, true),
                    new Columna("Diferencia", 420, a, true)));
            for (ArqueoPorMetodo.Linea l : r.arqueo().lineas()) {
                hoja.fila(List.of(ConsultaDeCierres.nombreDelMetodo(MetodoPagoCaja.valueOf(l.metodo())),
                        pesos(l.esperado()), l.contado() == null ? "-" : pesos(l.contado()),
                        l.diferencia() == null ? "-" : pesos(l.diferencia())), false);
            }
            hoja.linea();
            hoja.fila(List.of("Total", pesos(r.arqueo().totalEsperado()), pesos(r.arqueo().totalContado()),
                    pesos(r.arqueo().totalDiferencia())), true);

            if (r.observaciones() != null && !r.observaciones().isBlank()) {
                hoja.seccion("Observaciones");
                hoja.renglon(r.observaciones(), 10, false);
            }

            if (movs != null) {
                hoja.seccion("Movimientos");
                hoja.tabla(List.of(new Columna("Fecha", HojaPdf.MARGEN, 130, false),
                        new Columna("Tipo", 130, 250, false), new Columna("Documento", 250, 340, false),
                        new Columna("Método", 340, 440, false), new Columna("Monto", 440, a, true)));
                for (MovimientoEnCierre m : movs.movimientos()) {
                    hoja.fila(List.of(m.ocurridoEn() == null ? "" : fechaHora(m.ocurridoEn()),
                            m.tipoNombre() + (m.anulado() ? " (anulado)" : ""),
                            m.documento() == null ? "" : m.documento(), m.metodoNombre(), pesos(m.monto())), false);
                }
                hoja.linea();
                hoja.fila(List.of("Total", "", "", "", pesos(movs.total())), true);
            }
            String tipo = detallado ? "detallado" : "general";
            return new InformeEnPdf("cierre-" + archivo(c.numero()) + "-" + tipo + ".pdf", hoja.bytes());
        } catch (IOException fallo) {
            throw new ReglaDeNegocioException("No se pudo generar el PDF: " + fallo.getMessage());
        }
    }

    /** C3 · Todos los cierres del rango (las páginas que haga falta) y sus totales. */
    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public InformeEnPdf deCierres(LocalDate desde, LocalDate hasta, UUID cajeroId) {
        List<CierreEnBusqueda> cierres = new ArrayList<>();
        int pagina = 0;
        PaginaDeCierres p;
        do {
            p = consulta.buscar(desde, hasta, cajeroId, pagina++, ConsultaDeCierres.TAMANO_MAXIMO);
            cierres.addAll(p.cierres());
        } while (p.hayMas());

        BigDecimal esperado = BigDecimal.ZERO;
        BigDecimal diferencia = BigDecimal.ZERO;
        int descuadrados = 0;
        try (HojaPdf hoja = new HojaPdf(true)) {
            encabezado(hoja);
            hoja.titulo("CIERRES DE CAJA DEL " + FECHA.format(desde) + " AL " + FECHA.format(hasta));
            if (cajeroId != null && !cierres.isEmpty()) {
                hoja.subtitulo("Cajero: " + cierres.get(0).cajero());
            }
            hoja.espacio(6);
            float a = hoja.ancho();
            hoja.tabla(List.of(new Columna("Fecha de cierre", HojaPdf.MARGEN, 150, false),
                    new Columna("Caja", 150, 290, false), new Columna("Número", 290, 380, false),
                    new Columna("Cajero", 380, 520, false), new Columna("Esperado", 520, 610, true),
                    new Columna("Diferencia", 610, 700, true), new Columna("Estado", 715, a, false)));
            for (CierreEnBusqueda c : cierres) {
                hoja.fila(List.of(c.cerradaEn() == null ? "" : fechaHora(c.cerradaEn()), c.caja(), c.numero(),
                        c.cajero(), pesos(c.totalEsperado()), pesos(c.diferenciaTotal()), c.estado()), false);
                esperado = esperado.add(c.totalEsperado());
                diferencia = diferencia.add(c.diferenciaTotal());
                if (c.diferenciaTotal().signum() != 0) {
                    descuadrados++;
                }
            }
            hoja.linea();
            hoja.fila(List.of("Total del rango", cierres.size() == 1 ? "1 cierre" : cierres.size() + " cierres",
                    "", descuadrados == 0 ? "Sin descuadres" : descuadrados + " con diferencia", pesos(esperado),
                    pesos(diferencia), ""), true);
            if (cierres.isEmpty()) {
                hoja.espacio(6);
                hoja.renglon("No hubo cierres de caja en este rango.", 10, false);
            }
            return new InformeEnPdf("cierres-de-caja-" + desde + "-a-" + hasta + ".pdf", hoja.bytes());
        } catch (IOException fallo) {
            throw new ReglaDeNegocioException("No se pudo generar el PDF: " + fallo.getMessage());
        }
    }

    // ---------------------------------------------------------------------------------------------

    private void encabezado(HojaPdf hoja) throws IOException {
        DatosNegocioTicket datos = negocio == null ? null : negocio.datosTicket(ContextoDeNegocio.negocioActual());
        if (datos == null) {
            return;
        }
        String nombre = datos.nombreComercial() != null && !datos.nombreComercial().isBlank()
                ? datos.nombreComercial() : datos.razonSocial();
        if (nombre != null && !nombre.isBlank()) {
            hoja.renglon(nombre, 12, true);
        }
        if (datos.nit() != null && !datos.nit().isBlank()) {
            hoja.subtitulo("NIT " + datos.nit());
        }
        hoja.espacio(4);
    }

    private static String fechaHora(OffsetDateTime f) {
        return FECHA_HORA.format(f.atZoneSameInstant(ConsultaDeCierres.ZONA));
    }

    private static String archivo(String numero) {
        return numero == null ? "caja" : numero.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    /** Una salida: «-$ 40.000», o «$ 0» si no hubo. */
    private static String menos(BigDecimal v) {
        return pesos(v.negate());
    }

    /** «$ 1.250.000» y «-$ 2.000», sin decimales. */
    static String pesos(BigDecimal v) {
        if (v == null) {
            return "";
        }
        DecimalFormat formato = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO")));
        formato.setGroupingSize(3);
        DecimalFormatSymbols simbolos = formato.getDecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        formato.setDecimalFormatSymbols(simbolos);
        BigDecimal redondeado = v.setScale(0, RoundingMode.HALF_UP);
        return (redondeado.signum() < 0 ? "-$ " : "$ ") + formato.format(redondeado.abs());
    }
}
