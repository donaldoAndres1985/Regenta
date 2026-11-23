package com.regenta.compras.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.infrastructure.DevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.LineaDeDevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.archivos.ExportadorDeReportes;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-183 · Informes de compras y de costos (sección 7 de {@code docs/dominio/compras.md}).
 *
 * <ul>
 * <li><b>Por proveedor</b> (C1): base, IVA y total de lo comprado en el periodo, separando compras
 * directas y con orden. Solo cuentan las recepciones confirmadas (las anuladas no suman) y las
 * devoluciones del periodo restan con el valor de su nota crédito. La base es la de la factura ya
 * rebajado el descuento global; el total además lleva el flete.</li>
 * <li><b>Por producto</b> (C2): cantidad recibida menos devuelta, su costo de entrada total y el
 * costo de entrada promedio.</li>
 * <li><b>Historial de costos</b> (C3): cada vez que entró un producto, con fecha, proveedor y
 * recepción.</li>
 * </ul>
 *
 * <p>El periodo va por días completos en la hora de Colombia, la misma que usa el resto del
 * sistema por defecto. Todo exige {@code COMPRAS_INFORME_VER} (C5) y cualquier informe se descarga
 * en PDF o Excel (C4).
 */
@Service
public class InformesDeCompras {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RecepcionRepositorio recepciones;
    private final RecepcionLineaRepositorio lineas;
    private final DevolucionAProveedorRepositorio devoluciones;
    private final LineaDeDevolucionAProveedorRepositorio lineasDevueltas;
    private final ProveedorRepositorio proveedores;
    private final ExportadorDeReportes exportador;

    public InformesDeCompras(RecepcionRepositorio recepciones, RecepcionLineaRepositorio lineas,
            DevolucionAProveedorRepositorio devoluciones, LineaDeDevolucionAProveedorRepositorio lineasDevueltas,
            ProveedorRepositorio proveedores, ExportadorDeReportes exportador) {
        this.recepciones = recepciones;
        this.lineas = lineas;
        this.devoluciones = devoluciones;
        this.lineasDevueltas = lineasDevueltas;
        this.proveedores = proveedores;
        this.exportador = exportador;
    }

    /** Base, IVA y total. */
    public record Importe(BigDecimal base, BigDecimal iva, BigDecimal total) {

        static final Importe CERO = new Importe(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        Importe mas(BigDecimal b, BigDecimal i, BigDecimal t) {
            return new Importe(base.add(b), iva.add(i), total.add(t));
        }

        Importe menos(Importe otro) {
            return new Importe(base.subtract(otro.base), iva.subtract(otro.iva), total.subtract(otro.total));
        }

        Importe mas(Importe otro) {
            return mas(otro.base, otro.iva, otro.total);
        }

        Importe redondeado() {
            return new Importe(dos(base), dos(iva), dos(total));
        }
    }

    /** C1 · Lo comprado a un proveedor en el periodo; {@code neto} = directas + con orden − devoluciones. */
    public record CompraPorProveedor(UUID proveedorId, String proveedor, int recepciones, Importe directas,
            Importe conOrden, Importe devoluciones, Importe neto) {
    }

    /** C2 · Lo que entró de un producto en el periodo, neto de devoluciones. */
    public record CompraPorProducto(UUID productoId, String producto, BigDecimal cantidad, BigDecimal costoTotal,
            BigDecimal costoPromedio) {
    }

    /** C3 · Una entrada del producto: cuándo, de quién, en qué recepción y a qué costo. */
    public record CostoHistorico(OffsetDateTime fecha, UUID proveedorId, String proveedor, UUID recepcionId,
            String recepcion, boolean conOrden, BigDecimal cantidad, BigDecimal costoEntrada) {
    }

    /** C4 · Un informe ya convertido en archivo. */
    public record ArchivoDeInforme(String nombre, String tipo, byte[] contenido) {
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public List<CompraPorProveedor> porProveedor(LocalDate desde, LocalDate hasta) {
        return calcularPorProveedor(desde, hasta);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public List<CompraPorProducto> porProducto(LocalDate desde, LocalDate hasta) {
        return calcularPorProducto(desde, hasta);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public List<CostoHistorico> historialDeCostos(UUID productoId, LocalDate desde, LocalDate hasta) {
        return calcularHistorial(productoId, desde, hasta);
    }

    /** C4 · {@code informe}: por-proveedor, por-producto o historial-de-costos; {@code formato}: PDF o XLSX. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_INFORME_VER")
    public ArchivoDeInforme archivo(String informe, String formato, LocalDate desde, LocalDate hasta,
            UUID productoId) {
        String f = formato == null ? "" : formato.trim().toUpperCase(Locale.ROOT);
        if (!f.equals("PDF") && !f.equals("XLSX")) {
            throw new ReglaDeNegocioException("Los informes se descargan en PDF o XLSX");
        }
        String clave = informe == null ? "" : informe.trim().toLowerCase(Locale.ROOT);
        String titulo;
        List<String> columnas;
        List<List<Object>> filas = new ArrayList<>();
        switch (clave) {
            case "por-proveedor" -> {
                titulo = "Compras por proveedor " + periodo(desde, hasta);
                columnas = List.of("Proveedor", "Recepciones", "Directas base", "Directas IVA", "Directas total",
                        "Con orden base", "Con orden IVA", "Con orden total", "Devoluciones total", "Base",
                        "IVA", "Total");
                for (CompraPorProveedor c : calcularPorProveedor(desde, hasta)) {
                    filas.add(List.<Object>of(c.proveedor(), c.recepciones(), c.directas().base(), c.directas().iva(),
                            c.directas().total(), c.conOrden().base(), c.conOrden().iva(), c.conOrden().total(),
                            c.devoluciones().total(), c.neto().base(), c.neto().iva(), c.neto().total()));
                }
            }
            case "por-producto" -> {
                titulo = "Compras por producto " + periodo(desde, hasta);
                columnas = List.of("Producto", "Cantidad", "Costo total", "Costo promedio");
                for (CompraPorProducto c : calcularPorProducto(desde, hasta)) {
                    filas.add(List.<Object>of(c.producto(), c.cantidad(), c.costoTotal(),
                            c.costoPromedio() == null ? "" : c.costoPromedio()));
                }
            }
            case "historial-de-costos" -> {
                if (productoId == null) {
                    throw new ReglaDeNegocioException("Indica el producto del historial de costos");
                }
                List<CostoHistorico> historial = calcularHistorial(productoId, desde, hasta);
                String nombre = lineas.findByNegocioIdAndProductoId(ContextoDeNegocio.negocioActual(), productoId)
                        .stream().map(RecepcionLinea::getNombre).filter(n -> n != null && !n.isBlank())
                        .findFirst().orElse("producto");
                titulo = "Historial de costos " + nombre;
                columnas = List.of("Fecha", "Proveedor", "Recepcion", "Tipo", "Cantidad", "Costo de entrada");
                for (CostoHistorico c : historial) {
                    filas.add(List.<Object>of(FECHA.format(c.fecha().atZoneSameInstant(ZONA)), c.proveedor(),
                            c.recepcion(), c.conOrden() ? "Con orden" : "Directa", c.cantidad(), c.costoEntrada()));
                }
            }
            default -> throw new ReglaDeNegocioException(
                    "Informe desconocido: " + informe + ". Son por-proveedor, por-producto o historial-de-costos");
        }
        return new ArchivoDeInforme(exportador.nombreDeArchivo(titulo, f), exportador.tipoMime(f),
                exportador.generar(f, titulo, columnas, filas));
    }

    // ---------------------------------------------------------------------------------------------

    private List<CompraPorProveedor> calcularPorProveedor(LocalDate desde, LocalDate hasta) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = rango(desde, hasta);

        Map<UUID, Importe> directas = new HashMap<>();
        Map<UUID, Importe> conOrden = new HashMap<>();
        Map<UUID, Importe> devuelto = new HashMap<>();
        Map<UUID, Integer> cuantas = new HashMap<>();
        for (Recepcion r : confirmadas(negocio, rango)) {
            BigDecimal base = r.getSubtotal().subtract(r.getDescuentoGlobal());
            Map<UUID, Importe> destino = r.esCompraDirecta() ? directas : conOrden;
            destino.merge(r.getProveedorId(), Importe.CERO.mas(base, r.getImpuestoTotal(), r.getTotal()),
                    Importe::mas);
            cuantas.merge(r.getProveedorId(), 1, Integer::sum);
        }
        List<DevolucionAProveedor> devs = devoluciones.findByNegocioIdAndFechaGreaterThanEqualAndFechaLessThan(
                negocio, rango[0], rango[1]);
        Map<UUID, List<LineaDeDevolucionAProveedor>> lineasPorDevolucion = lineasDe(devs);
        for (DevolucionAProveedor d : devs) {
            BigDecimal base = BigDecimal.ZERO;
            BigDecimal iva = BigDecimal.ZERO;
            for (LineaDeDevolucionAProveedor l : lineasPorDevolucion.getOrDefault(d.getId(), List.of())) {
                base = base.add(l.getBase());
                iva = iva.add(l.getImpuesto());
            }
            devuelto.merge(d.getProveedorId(), Importe.CERO.mas(base, iva, d.getValorNota()), Importe::mas);
        }

        Set<UUID> ids = new java.util.HashSet<>();
        ids.addAll(directas.keySet());
        ids.addAll(conOrden.keySet());
        ids.addAll(devuelto.keySet());
        Map<UUID, String> nombres = nombresDeProveedores(ids);
        List<CompraPorProveedor> filas = new ArrayList<>();
        for (UUID id : ids) {
            Importe d = directas.getOrDefault(id, Importe.CERO);
            Importe o = conOrden.getOrDefault(id, Importe.CERO);
            Importe v = devuelto.getOrDefault(id, Importe.CERO);
            filas.add(new CompraPorProveedor(id, nombres.getOrDefault(id, "Proveedor"), cuantas.getOrDefault(id, 0),
                    d.redondeado(), o.redondeado(), v.redondeado(), d.mas(o).menos(v).redondeado()));
        }
        filas.sort(Comparator.comparing((CompraPorProveedor c) -> c.neto().total()).reversed()
                .thenComparing(CompraPorProveedor::proveedor));
        return filas;
    }

    private List<CompraPorProducto> calcularPorProducto(LocalDate desde, LocalDate hasta) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = rango(desde, hasta);

        Map<UUID, BigDecimal> cantidad = new LinkedHashMap<>();
        Map<UUID, BigDecimal> costo = new HashMap<>();
        Map<UUID, String> nombre = new HashMap<>();
        List<UUID> ids = confirmadas(negocio, rango).stream().map(Recepcion::getId).toList();
        if (!ids.isEmpty()) {
            for (RecepcionLinea l : lineas.findByRecepcionIdIn(ids)) {
                BigDecimal unitario = l.getCostoEntrada() == null ? l.costoNeto() : l.getCostoEntrada();
                cantidad.merge(l.getProductoId(), l.getCantidad(), BigDecimal::add);
                costo.merge(l.getProductoId(), l.getCantidad().multiply(unitario), BigDecimal::add);
                if (l.getNombre() != null) {
                    nombre.putIfAbsent(l.getProductoId(), l.getNombre());
                }
            }
        }
        List<DevolucionAProveedor> devs = devoluciones.findByNegocioIdAndFechaGreaterThanEqualAndFechaLessThan(
                negocio, rango[0], rango[1]);
        for (List<LineaDeDevolucionAProveedor> ls : lineasDe(devs).values()) {
            for (LineaDeDevolucionAProveedor l : ls) {
                cantidad.merge(l.getProductoId(), l.getCantidad().negate(), BigDecimal::add);
                costo.merge(l.getProductoId(), l.getCantidad().multiply(l.getCostoEntrada()).negate(),
                        BigDecimal::add);
                nombre.putIfAbsent(l.getProductoId(), l.getNombre());
            }
        }

        List<CompraPorProducto> filas = new ArrayList<>();
        for (Map.Entry<UUID, BigDecimal> e : cantidad.entrySet()) {
            BigDecimal total = costo.getOrDefault(e.getKey(), BigDecimal.ZERO);
            BigDecimal promedio = e.getValue().signum() > 0
                    ? total.divide(e.getValue(), 2, RoundingMode.HALF_UP)
                    : null;
            filas.add(new CompraPorProducto(e.getKey(), nombre.getOrDefault(e.getKey(), "Producto"),
                    cantidad(e.getValue()), dos(total), promedio));
        }
        filas.sort(Comparator.comparing(CompraPorProducto::costoTotal).reversed()
                .thenComparing(CompraPorProducto::producto));
        return filas;
    }

    private List<CostoHistorico> calcularHistorial(UUID productoId, LocalDate desde, LocalDate hasta) {
        if (productoId == null) {
            throw new ReglaDeNegocioException("Indica el producto del historial de costos");
        }
        UUID negocio = ContextoDeNegocio.negocioActual();
        OffsetDateTime[] rango = desde == null && hasta == null ? null : rango(desde, hasta);
        List<RecepcionLinea> entradas = lineas.findByNegocioIdAndProductoId(negocio, productoId);
        Set<UUID> ids = entradas.stream().map(RecepcionLinea::getRecepcionId).collect(Collectors.toSet());
        Map<UUID, Recepcion> porId = new HashMap<>();
        for (Recepcion r : recepciones.findAllById(ids)) {
            if (negocio.equals(r.getNegocioId()) && r.getEstado() == EstadoRecepcion.CONFIRMADA
                    && (rango == null || (!r.getFecha().isBefore(rango[0]) && r.getFecha().isBefore(rango[1])))) {
                porId.put(r.getId(), r);
            }
        }
        Map<UUID, String> nombres = nombresDeProveedores(
                porId.values().stream().map(Recepcion::getProveedorId).collect(Collectors.toSet()));
        List<CostoHistorico> historial = new ArrayList<>();
        for (RecepcionLinea l : entradas) {
            Recepcion r = porId.get(l.getRecepcionId());
            if (r == null) {
                continue;
            }
            BigDecimal unitario = l.getCostoEntrada() == null ? l.costoNeto() : l.getCostoEntrada();
            historial.add(new CostoHistorico(r.getFecha(), r.getProveedorId(),
                    nombres.getOrDefault(r.getProveedorId(), "Proveedor"), r.getId(), r.getNumero(),
                    !r.esCompraDirecta(), cantidad(l.getCantidad()), dos(unitario)));
        }
        historial.sort(Comparator.comparing(CostoHistorico::fecha).reversed());
        return historial;
    }

    private List<Recepcion> confirmadas(UUID negocio, OffsetDateTime[] rango) {
        return recepciones.findByNegocioIdAndEstadoAndFechaGreaterThanEqualAndFechaLessThan(negocio,
                EstadoRecepcion.CONFIRMADA, rango[0], rango[1]);
    }

    private Map<UUID, List<LineaDeDevolucionAProveedor>> lineasDe(List<DevolucionAProveedor> devs) {
        if (devs.isEmpty()) {
            return Map.of();
        }
        return lineasDevueltas.findByDevolucionIdIn(devs.stream().map(DevolucionAProveedor::getId).toList())
                .stream().collect(Collectors.groupingBy(LineaDeDevolucionAProveedor::getDevolucionId));
    }

    private Map<UUID, String> nombresDeProveedores(Set<UUID> ids) {
        Map<UUID, String> nombres = new HashMap<>();
        if (ids.isEmpty()) {
            return nombres;
        }
        for (Proveedor p : proveedores.findAllById(ids)) {
            String n = p.getNombreComercial();
            nombres.put(p.getId(), n == null || n.isBlank() ? p.getRazonSocial() : n);
        }
        return nombres;
    }

    /** [desde 00:00, día siguiente a hasta 00:00) en la hora del negocio. Sin fechas: el mes en curso. */
    static OffsetDateTime[] rango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(ZONA);
        LocalDate d = desde != null ? desde : (hasta != null ? hasta.withDayOfMonth(1) : hoy.withDayOfMonth(1));
        LocalDate h = hasta != null ? hasta : hoy;
        if (h.isBefore(d)) {
            throw new ReglaDeNegocioException("El periodo termina antes de empezar: revisa las fechas");
        }
        return new OffsetDateTime[] {d.atStartOfDay(ZONA).toOffsetDateTime(),
            h.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime()};
    }

    private static String periodo(LocalDate desde, LocalDate hasta) {
        OffsetDateTime[] r = rango(desde, hasta);
        return FECHA.format(r[0].atZoneSameInstant(ZONA)) + " a "
                + FECHA.format(r[1].minusDays(1).atZoneSameInstant(ZONA));
    }

    /** 10 y no 10.0000 ni 1E+1: así sale en la pantalla, el PDF y el Excel. */
    private static BigDecimal cantidad(BigDecimal v) {
        BigDecimal limpia = v.stripTrailingZeros();
        return limpia.scale() < 0 ? limpia.setScale(0) : limpia;
    }

    private static BigDecimal dos(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
