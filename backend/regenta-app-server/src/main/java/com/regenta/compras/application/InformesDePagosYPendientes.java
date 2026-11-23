package com.regenta.compras.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.application.InformesDeCompras.ArchivoDeInforme;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.shared.archivos.ExportadorDeReportes;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-184 · Cuánto se debe y desde cuándo, qué falta por recibir y qué se pagó (sección 7 de
 * {@code docs/dominio/compras.md}).
 *
 * <ul>
 * <li><b>Antigüedad</b> (C1): el saldo abierto de cada proveedor repartido por días de mora a hoy
 * —al día, 1-30, 31-60, 61-90 y más de 90— con el total de cada columna. La mora se calcula (I25):
 * no se lee un estado guardado.</li>
 * <li><b>Pendientes por recibir</b> (C2): de las órdenes aprobadas, enviadas o parciales, lo que
 * falta de cada línea y la fecha esperada; las atrasadas van primero y marcadas.</li>
 * <li><b>Faltantes</b> (C3): lo que nunca llegó de las órdenes cerradas con faltante.</li>
 * <li><b>Pagos</b> (C4): los pagos del periodo por proveedor y método, sin los anulados.</li>
 * </ul>
 *
 * <p>Como los de HU-183, exigen {@code COMPRAS_INFORME_VER} y se descargan en PDF o Excel.
 */
@Service
public class InformesDePagosYPendientes {

    /** Los informes de esta clase, como van en la ruta {@code /api/compras/informes/{informe}/archivo}. */
    public static final Set<String> INFORMES = Set.of("antiguedad", "pendientes-por-recibir", "faltantes", "pagos");

    private static final Set<EstadoOrdenCompra> POR_RECIBIR =
            Set.of(EstadoOrdenCompra.APROBADA, EstadoOrdenCompra.ENVIADA, EstadoOrdenCompra.PARCIAL);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final CuentaPorPagarRepositorio cuentas;
    private final OrdenDeCompraRepositorio ordenes;
    private final OrdenCompraLineaRepositorio lineas;
    private final PagoProveedorRepositorio pagos;
    private final ProveedorRepositorio proveedores;
    private final ExportadorDeReportes exportador;

    public InformesDePagosYPendientes(CuentaPorPagarRepositorio cuentas, OrdenDeCompraRepositorio ordenes,
            OrdenCompraLineaRepositorio lineas, PagoProveedorRepositorio pagos, ProveedorRepositorio proveedores,
            ExportadorDeReportes exportador) {
        this.cuentas = cuentas;
        this.ordenes = ordenes;
        this.lineas = lineas;
        this.pagos = pagos;
        this.proveedores = proveedores;
        this.exportador = exportador;
    }

    /** C1 · Un saldo repartido por días de mora. */
    public record Tramos(BigDecimal alDia, BigDecimal de1a30, BigDecimal de31a60, BigDecimal de61a90,
            BigDecimal masDe90, BigDecimal total) {

        static final Tramos CERO = new Tramos(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);

        Tramos sumar(long diasDeMora, BigDecimal saldo) {
            return new Tramos(
                    diasDeMora <= 0 ? alDia.add(saldo) : alDia,
                    diasDeMora >= 1 && diasDeMora <= 30 ? de1a30.add(saldo) : de1a30,
                    diasDeMora >= 31 && diasDeMora <= 60 ? de31a60.add(saldo) : de31a60,
                    diasDeMora >= 61 && diasDeMora <= 90 ? de61a90.add(saldo) : de61a90,
                    diasDeMora > 90 ? masDe90.add(saldo) : masDe90,
                    total.add(saldo));
        }

        Tramos mas(Tramos o) {
            return new Tramos(alDia.add(o.alDia), de1a30.add(o.de1a30), de31a60.add(o.de31a60),
                    de61a90.add(o.de61a90), masDe90.add(o.masDe90), total.add(o.total));
        }

        Tramos redondeados() {
            return new Tramos(dos(alDia), dos(de1a30), dos(de31a60), dos(de61a90), dos(masDe90), dos(total));
        }
    }

    /** C1 · Lo que se le debe a un proveedor, por antigüedad, y la mora más vieja. */
    public record AntiguedadDeProveedor(UUID proveedorId, String proveedor, int cuentas, long moraMaxima,
            Tramos saldo) {
    }

    /** C1 · El informe completo: una fila por proveedor y el total de cada columna. */
    public record InformeDeAntiguedad(LocalDate fecha, List<AntiguedadDeProveedor> proveedores, Tramos totales) {
    }

    /** C2 / C3 · Una línea de orden con lo que falta: {@code valor} = faltante × costo con descuento, sin IVA. */
    public record LineaPendiente(int linea, UUID productoId, String producto, BigDecimal pedida,
            BigDecimal recibida, BigDecimal faltante, BigDecimal costoUnitario, BigDecimal valor) {
    }

    /** C2 · Una orden por recibir; atrasada si su fecha esperada ya pasó. */
    public record PendientePorRecibir(UUID ordenId, String numero, UUID proveedorId, String proveedor,
            EstadoOrdenCompra estado, LocalDate fechaEsperada, boolean atrasada, long diasDeAtraso,
            List<LineaPendiente> lineas, BigDecimal valorPendiente) {
    }

    /** C3 · Una orden cerrada con faltante y lo que nunca llegó. */
    public record FaltanteDeOrden(UUID ordenId, String numero, UUID proveedorId, String proveedor,
            OffsetDateTime cerradaEn, String motivo, List<LineaPendiente> lineas, BigDecimal valorFaltante) {
    }

    /** C4 · Los pagos a un proveedor con un método en el periodo. */
    public record PagosPorProveedorYMetodo(UUID proveedorId, String proveedor, MetodoDePago metodo, int pagos,
            BigDecimal total) {
    }

    /** C4 · El informe de pagos: filas por proveedor y método, totales por método y total general. */
    public record InformeDePagos(LocalDate desde, LocalDate hasta, List<PagosPorProveedorYMetodo> filas,
            Map<MetodoDePago, BigDecimal> porMetodo, BigDecimal total) {
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public InformeDeAntiguedad antiguedad() {
        return calcularAntiguedad(hoy());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public List<PendientePorRecibir> pendientesPorRecibir() {
        return calcularPendientes(hoy());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public List<FaltanteDeOrden> faltantes(LocalDate desde, LocalDate hasta) {
        return calcularFaltantes(desde, hasta);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public InformeDePagos pagos(LocalDate desde, LocalDate hasta) {
        return calcularPagos(desde, hasta);
    }

    /** PDF o XLSX de antiguedad, pendientes-por-recibir, faltantes o pagos. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public ArchivoDeInforme archivo(String informe, String formato, LocalDate desde, LocalDate hasta) {
        String f = formato == null ? "" : formato.trim().toUpperCase(Locale.ROOT);
        if (!f.equals("PDF") && !f.equals("XLSX")) {
            throw new ReglaDeNegocioException("Los informes se descargan en PDF o XLSX");
        }
        LocalDate hoy = hoy();
        String titulo;
        List<String> columnas;
        List<List<Object>> filas = new ArrayList<>();
        switch (informe == null ? "" : informe.trim().toLowerCase(Locale.ROOT)) {
            case "antiguedad" -> {
                InformeDeAntiguedad a = calcularAntiguedad(hoy);
                titulo = "Cuentas por pagar por antiguedad al " + FECHA.format(hoy);
                columnas = List.of("Proveedor", "Al dia", "1-30", "31-60", "61-90", "Mas de 90", "Total");
                for (AntiguedadDeProveedor p : a.proveedores()) {
                    filas.add(fila(p.proveedor(), p.saldo()));
                }
                filas.add(fila("TOTAL", a.totales()));
            }
            case "pendientes-por-recibir" -> {
                titulo = "Pendientes por recibir al " + FECHA.format(hoy);
                columnas = List.of("Orden", "Proveedor", "Fecha esperada", "Atrasada", "Producto", "Pedida",
                        "Recibida", "Falta", "Valor");
                for (PendientePorRecibir o : calcularPendientes(hoy)) {
                    for (LineaPendiente l : o.lineas()) {
                        filas.add(List.<Object>of(o.numero(), o.proveedor(),
                                o.fechaEsperada() == null ? "" : FECHA.format(o.fechaEsperada()),
                                o.atrasada() ? o.diasDeAtraso() + " dias" : "", l.producto(), l.pedida(),
                                l.recibida(), l.faltante(), l.valor()));
                    }
                }
            }
            case "faltantes" -> {
                titulo = "Faltantes de ordenes cerradas";
                columnas = List.of("Orden", "Proveedor", "Cerrada", "Motivo", "Producto", "Pedida", "Recibida",
                        "Nunca llego", "Valor");
                for (FaltanteDeOrden o : calcularFaltantes(desde, hasta)) {
                    for (LineaPendiente l : o.lineas()) {
                        filas.add(List.<Object>of(o.numero(), o.proveedor(),
                                o.cerradaEn() == null ? ""
                                        : FECHA.format(o.cerradaEn().atZoneSameInstant(InformesDeCompras.ZONA)),
                                o.motivo() == null ? "" : o.motivo(), l.producto(), l.pedida(), l.recibida(),
                                l.faltante(), l.valor()));
                    }
                }
            }
            case "pagos" -> {
                InformeDePagos p = calcularPagos(desde, hasta);
                titulo = "Pagos a proveedores " + FECHA.format(p.desde()) + " a " + FECHA.format(p.hasta());
                columnas = List.of("Proveedor", "Metodo", "Pagos", "Total");
                for (PagosPorProveedorYMetodo r : p.filas()) {
                    filas.add(List.<Object>of(r.proveedor(), r.metodo().name(), r.pagos(), r.total()));
                }
                for (Map.Entry<MetodoDePago, BigDecimal> e : p.porMetodo().entrySet()) {
                    filas.add(List.<Object>of("TOTAL", e.getKey().name(), "", e.getValue()));
                }
                filas.add(List.<Object>of("TOTAL", "", "", p.total()));
            }
            default -> throw new ReglaDeNegocioException("Informe desconocido: " + informe);
        }
        return new ArchivoDeInforme(exportador.nombreDeArchivo(titulo, f), exportador.tipoMime(f),
                exportador.generar(f, titulo, columnas, filas));
    }

    // ---------------------------------------------------------------------------------------------

    InformeDeAntiguedad calcularAntiguedad(LocalDate hoy) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        Map<UUID, Tramos> porProveedor = new HashMap<>();
        Map<UUID, Integer> cuantas = new HashMap<>();
        Map<UUID, Long> mora = new HashMap<>();
        for (CuentaPorPagar c : cuentas.findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(negocio)) {
            if (c.getSaldo().signum() <= 0 || c.getEstado() == EstadoCuentaPorPagar.ANULADA
                    || c.getEstado() == EstadoCuentaPorPagar.PAGADA) {
                continue;
            }
            long dias = c.diasDeMora(hoy);
            porProveedor.merge(c.getProveedorId(), Tramos.CERO.sumar(dias, c.getSaldo()), Tramos::mas);
            cuantas.merge(c.getProveedorId(), 1, Integer::sum);
            mora.merge(c.getProveedorId(), dias, Math::max);
        }
        Map<UUID, String> nombres = nombres(porProveedor.keySet());
        List<AntiguedadDeProveedor> filas = new ArrayList<>();
        Tramos totales = Tramos.CERO;
        for (Map.Entry<UUID, Tramos> e : porProveedor.entrySet()) {
            filas.add(new AntiguedadDeProveedor(e.getKey(), nombres.getOrDefault(e.getKey(), "Proveedor"),
                    cuantas.get(e.getKey()), mora.get(e.getKey()), e.getValue().redondeados()));
            totales = totales.mas(e.getValue());
        }
        // El que más debe en mora vieja, primero.
        filas.sort(Comparator.comparingLong(AntiguedadDeProveedor::moraMaxima).reversed()
                .thenComparing((AntiguedadDeProveedor a) -> a.saldo().total(), Comparator.reverseOrder())
                .thenComparing(AntiguedadDeProveedor::proveedor));
        return new InformeDeAntiguedad(hoy, filas, totales.redondeados());
    }

    List<PendientePorRecibir> calcularPendientes(LocalDate hoy) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<OrdenDeCompra> abiertas = ordenes.findByNegocioIdAndEstadoIn(negocio, POR_RECIBIR);
        Map<UUID, List<LineaPendiente>> pendientes = lineasConFaltante(abiertas);
        Map<UUID, String> nombres = nombres(abiertas.stream().map(OrdenDeCompra::getProveedorId)
                .collect(Collectors.toSet()));
        List<PendientePorRecibir> filas = new ArrayList<>();
        for (OrdenDeCompra o : abiertas) {
            List<LineaPendiente> ls = pendientes.getOrDefault(o.getId(), List.of());
            if (ls.isEmpty()) {
                continue;
            }
            LocalDate esperada = o.getFechaEsperada();
            boolean atrasada = esperada != null && esperada.isBefore(hoy);
            filas.add(new PendientePorRecibir(o.getId(), o.getNumero(), o.getProveedorId(),
                    nombres.getOrDefault(o.getProveedorId(), "Proveedor"), o.getEstado(), esperada, atrasada,
                    atrasada ? ChronoUnit.DAYS.between(esperada, hoy) : 0, ls, sumaDeValores(ls)));
        }
        // Las atrasadas primero (la más atrasada arriba); después por fecha esperada, sin fecha al final.
        filas.sort(Comparator.comparing(PendientePorRecibir::atrasada).reversed()
                .thenComparing(PendientePorRecibir::fechaEsperada, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(PendientePorRecibir::numero));
        return filas;
    }

    List<FaltanteDeOrden> calcularFaltantes(LocalDate desde, LocalDate hasta) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = desde == null && hasta == null ? null : InformesDeCompras.rango(desde, hasta);
        List<OrdenDeCompra> cerradas = ordenes.findByNegocioIdAndEstadoIn(negocio, Set.of(EstadoOrdenCompra.CERRADA))
                .stream()
                .filter(o -> rango == null || (o.getCerradaEn() != null && !o.getCerradaEn().isBefore(rango[0])
                        && o.getCerradaEn().isBefore(rango[1])))
                .toList();
        Map<UUID, List<LineaPendiente>> faltan = lineasConFaltante(cerradas);
        Map<UUID, String> nombres = nombres(cerradas.stream().map(OrdenDeCompra::getProveedorId)
                .collect(Collectors.toSet()));
        List<FaltanteDeOrden> filas = new ArrayList<>();
        for (OrdenDeCompra o : cerradas) {
            List<LineaPendiente> ls = faltan.getOrDefault(o.getId(), List.of());
            if (!ls.isEmpty()) {
                filas.add(new FaltanteDeOrden(o.getId(), o.getNumero(), o.getProveedorId(),
                        nombres.getOrDefault(o.getProveedorId(), "Proveedor"), o.getCerradaEn(),
                        o.getMotivoCierre(), ls, sumaDeValores(ls)));
            }
        }
        filas.sort(Comparator.comparing(FaltanteDeOrden::cerradaEn, Comparator.nullsLast(Comparator.reverseOrder())));
        return filas;
    }

    InformeDePagos calcularPagos(LocalDate desde, LocalDate hasta) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        LocalDate hoy = hoy();
        LocalDate d = desde != null ? desde : (hasta != null ? hasta.withDayOfMonth(1) : hoy.withDayOfMonth(1));
        LocalDate h = hasta != null ? hasta : hoy;
        if (h.isBefore(d)) {
            throw new ReglaDeNegocioException("El periodo termina antes de empezar: revisa las fechas");
        }
        record Clave(UUID proveedorId, MetodoDePago metodo) {
        }
        Map<Clave, BigDecimal> totales = new LinkedHashMap<>();
        Map<Clave, Integer> cuantos = new HashMap<>();
        Map<MetodoDePago, BigDecimal> porMetodo = new EnumMap<>(MetodoDePago.class);
        BigDecimal total = BigDecimal.ZERO;
        for (PagoProveedor p : pagos.findByNegocioIdAndAnuladoEnIsNullAndFechaBetween(negocio, d, h)) {
            if (p.estaAnulado()) {
                continue;
            }
            Clave k = new Clave(p.getProveedorId(), p.getMetodo());
            totales.merge(k, p.getMonto(), BigDecimal::add);
            cuantos.merge(k, 1, Integer::sum);
            porMetodo.merge(p.getMetodo(), p.getMonto(), BigDecimal::add);
            total = total.add(p.getMonto());
        }
        Map<UUID, String> nombres = nombres(totales.keySet().stream().map(Clave::proveedorId)
                .collect(Collectors.toSet()));
        List<PagosPorProveedorYMetodo> filas = new ArrayList<>();
        totales.forEach((k, v) -> filas.add(new PagosPorProveedorYMetodo(k.proveedorId(),
                nombres.getOrDefault(k.proveedorId(), "Proveedor"), k.metodo(), cuantos.get(k), dos(v))));
        filas.sort(Comparator.comparing(PagosPorProveedorYMetodo::proveedor)
                .thenComparing(PagosPorProveedorYMetodo::metodo));
        porMetodo.replaceAll((m, v) -> dos(v));
        return new InformeDePagos(d, h, filas, porMetodo, dos(total));
    }

    /** Las líneas con algo por llegar de cada orden, en su orden. */
    private Map<UUID, List<LineaPendiente>> lineasConFaltante(List<OrdenDeCompra> deLasOrdenes) {
        if (deLasOrdenes.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<LineaPendiente>> porOrden = new HashMap<>();
        for (OrdenCompraLinea l : lineas.findByOrdenIdInOrderByLinea(
                deLasOrdenes.stream().map(OrdenDeCompra::getId).toList())) {
            BigDecimal falta = l.faltante();
            if (falta.signum() <= 0) {
                continue;
            }
            BigDecimal descuento = l.getDescuentoPct() == null ? BigDecimal.ZERO : l.getDescuentoPct();
            BigDecimal neto = l.getCostoUnitario()
                    .multiply(BigDecimal.ONE.subtract(descuento.divide(CIEN, 8, RoundingMode.HALF_UP)));
            porOrden.computeIfAbsent(l.getOrdenId(), k -> new ArrayList<>()).add(new LineaPendiente(l.getLinea(),
                    l.getProductoId(), l.getNombreSnapshot() == null ? "Producto" : l.getNombreSnapshot(),
                    cantidad(l.getCantidadPedida()), cantidad(l.getCantidadRecibida()), cantidad(falta),
                    dos(l.getCostoUnitario()), dos(falta.multiply(neto))));
        }
        porOrden.values().forEach(ls -> ls.sort(Comparator.comparingInt(LineaPendiente::linea)));
        return porOrden;
    }

    private static BigDecimal sumaDeValores(List<LineaPendiente> ls) {
        return ls.stream().map(LineaPendiente::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<UUID, String> nombres(Set<UUID> ids) {
        Map<UUID, String> nombres = new HashMap<>();
        if (ids.isEmpty()) {
            return nombres;
        }
        for (Proveedor p : proveedores.findAllById(new HashSet<>(ids))) {
            String n = p.getNombreComercial();
            nombres.put(p.getId(), n == null || n.isBlank() ? p.getRazonSocial() : n);
        }
        return nombres;
    }

    private static List<Object> fila(String nombre, Tramos t) {
        return List.of(nombre, t.alDia(), t.de1a30(), t.de31a60(), t.de61a90(), t.masDe90(), t.total());
    }

    private static LocalDate hoy() {
        return LocalDate.now(InformesDeCompras.ZONA);
    }

    private static BigDecimal cantidad(BigDecimal v) {
        BigDecimal limpia = v.stripTrailingZeros();
        return limpia.scale() < 0 ? limpia.setScale(0) : limpia;
    }

    private static BigDecimal dos(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
