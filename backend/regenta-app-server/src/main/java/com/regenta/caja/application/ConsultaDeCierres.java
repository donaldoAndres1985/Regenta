package com.regenta.caja.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.Caja;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.CajaRepositorio;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/**
 * HU-143 · Consultar los cierres de caja pasados y su detalle, sin depender de la memoria del cajero.
 * Amplía HU-063: filtro por cajero, paginación sin tope fijo y el detalle de cada movimiento.
 *
 * <ul>
 * <li>C1 · Cierres de un rango (por fecha de cierre, en hora de Colombia), opcionalmente de un
 * cajero —el que abrió o el que cerró—, el más reciente primero y paginados.</li>
 * <li>C2 · El resumen de un cierre: ventas al contado y a crédito, ingresos, retiros, gastos y el
 * arqueo por método. Un movimiento anulado se compensa con su anulación, así que no suma.</li>
 * <li>C3 · Sus movimientos con el tipo en palabras y el total al final.</li>
 * <li>C4 / C5 · Un movimiento: quién lo registró, quién lo autorizó o anuló, la venta de la que
 * viene y, si fue un cobro mixto, todos sus métodos.</li>
 * <li>C6 · Todo se busca dentro del negocio actual.</li>
 * </ul>
 */
@Service
public class ConsultaDeCierres {

    static final ZoneId ZONA = ZoneId.of("America/Bogota");
    static final int TAMANO_POR_DEFECTO = 20;
    static final int TAMANO_MAXIMO = 100;

    private final SesionDeCajaRepositorio sesiones;
    private final MovimientoDeCajaRepositorio movimientos;
    private final ArqueoMetodoRepositorio arqueo;
    private final CajaRepositorio cajas;
    private final DirectorioDeUsuariosApi directorio;

    public ConsultaDeCierres(SesionDeCajaRepositorio sesiones, MovimientoDeCajaRepositorio movimientos,
            ArqueoMetodoRepositorio arqueo, CajaRepositorio cajas, DirectorioDeUsuariosApi directorio) {
        this.sesiones = sesiones;
        this.movimientos = movimientos;
        this.arqueo = arqueo;
        this.cajas = cajas;
        this.directorio = directorio;
    }

    /** C1 · Una fila del buscador de cierres. */
    public record CierreEnBusqueda(UUID sesionId, OffsetDateTime cerradaEn, UUID cajaId, String caja,
            String numero, UUID cajeroId, String cajero, BigDecimal totalEsperado, BigDecimal diferenciaTotal,
            String estado) {
    }

    /** C1 · Una página de cierres; {@code pagina} empieza en 0. */
    public record PaginaDeCierres(List<CierreEnBusqueda> cierres, int pagina, int tamano, long total,
            boolean hayMas) {
    }

    /** Un cajero que ha abierto caja en el negocio, para el filtro. */
    public record Cajero(UUID id, String nombre) {
    }

    /** C2 · Los totales de un cierre; las salidas van en positivo. */
    public record ResumenDeCierre(CierreEnBusqueda cierre, String cerradoPor, OffsetDateTime abiertaEn,
            BigDecimal montoApertura, BigDecimal ventasContado, BigDecimal ventasCredito, BigDecimal devoluciones,
            BigDecimal recaudos, BigDecimal ingresos, BigDecimal retiros, BigDecimal gastos,
            BigDecimal pagosAProveedor, ArqueoPorMetodo arqueo, String observaciones) {
    }

    /** C3 · Un movimiento en la lista del cierre; {@code monto} con su signo. */
    public record MovimientoEnCierre(UUID id, OffsetDateTime ocurridoEn, String tipo, String tipoNombre,
            String documento, String metodo, String metodoNombre, BigDecimal monto, boolean anulado) {
    }

    /** C3 · Los movimientos del cierre y la fila del total. */
    public record MovimientosDeCierre(UUID sesionId, List<MovimientoEnCierre> movimientos, BigDecimal total) {
    }

    /** C5 · Uno de los métodos de un cobro. */
    public record PagoDelCobro(UUID movimientoId, String metodo, String metodoNombre, BigDecimal monto,
            BigDecimal propina) {
    }

    /**
     * C4 · Un movimiento con todo: quién, cuándo, con qué, por qué. {@code ventaId} viene cuando nació de
     * una venta, para ir a ella. {@code pagos} trae todos los métodos del mismo cobro (C5).
     */
    public record DetalleDeMovimiento(UUID id, UUID sesionId, String tipo, String tipoNombre, String documento,
            OffsetDateTime ocurridoEn, String metodo, String metodoNombre, BigDecimal monto, BigDecimal propina,
            String concepto, String registradoPor, String autorizadoPor, String anuladoPor,
            OffsetDateTime anuladoEn, String motivoAnulacion, UUID anulaA, String origenTipo, UUID origenId,
            UUID ventaId, List<PagoDelCobro> pagos) {
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public PaginaDeCierres buscar(LocalDate desde, LocalDate hasta, UUID cajeroId, Integer pagina, Integer tamano) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El rango de fechas no es válido");
        }
        int p = pagina == null ? 0 : pagina;
        int t = tamano == null ? TAMANO_POR_DEFECTO : tamano;
        if (p < 0 || t < 1 || t > TAMANO_MAXIMO) {
            throw new ReglaDeNegocioException("La página empieza en 0 y trae de 1 a " + TAMANO_MAXIMO + " cierres");
        }
        UUID negocio = ContextoDeNegocio.negocioActual();
        Page<SesionDeCaja> encontrados = sesiones.cierres(negocio, desde.atStartOfDay(ZONA).toOffsetDateTime(),
                hasta.plusDays(1).atStartOfDay(ZONA).toOffsetDateTime(), cajeroId, PageRequest.of(p, t));
        List<CierreEnBusqueda> filas = filas(negocio, encontrados.getContent());
        return new PaginaDeCierres(filas, p, t, encontrados.getTotalElements(), encontrados.hasNext());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public List<Cajero> cajeros() {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<UUID> ids = sesiones.cajerosDelNegocio(negocio);
        Map<UUID, String> nombres = ids.isEmpty() ? Map.of() : directorio.nombresDe(negocio, ids);
        return ids.stream().map(id -> new Cajero(id, nombres.getOrDefault(id, "Usuario")))
                .sorted(Comparator.comparing(Cajero::nombre)).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public ResumenDeCierre resumen(UUID sesionId) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        SesionDeCaja s = cierre(negocio, sesionId);
        CierreEnBusqueda fila = filas(negocio, List.of(s)).get(0);
        String cerradoPor = s.getUsuarioCierreId() == null ? null
                : directorio.nombresDe(negocio, List.of(s.getUsuarioCierreId())).get(s.getUsuarioCierreId());

        List<MovimientoDeCaja> movs = movimientos.findBySesionIdOrderByOcurridoEnAsc(sesionId);
        Map<UUID, TipoMovimientoCaja> tipoDe = movs.stream()
                .collect(Collectors.toMap(MovimientoDeCaja::getId, MovimientoDeCaja::getTipo, (a, b) -> a));
        Map<String, BigDecimal> suma = new HashMap<>();
        for (MovimientoDeCaja m : movs) {
            // Una anulación cuenta como el tipo que anula, con el signo contrario: lo deja en cero.
            TipoMovimientoCaja tipo = m.getTipo() == TipoMovimientoCaja.ANULACION && m.getAnulaA() != null
                    ? tipoDe.getOrDefault(m.getAnulaA(), TipoMovimientoCaja.ANULACION)
                    : m.getTipo();
            String rubro = switch (tipo) {
                case VENTA, COMANDA, RESERVA -> m.getMetodoPago() == MetodoPagoCaja.CREDITO ? "credito" : "contado";
                case DEVOLUCION -> "devoluciones";
                case RECAUDO -> "recaudos";
                case INGRESO -> "ingresos";
                case RETIRO -> "retiros";
                case GASTO -> "gastos";
                case PAGO_PROVEEDOR -> "proveedores";
                default -> "otros";
            };
            suma.merge(rubro, conSigno(m), BigDecimal::add);
        }
        List<ArqueoPorMetodo.Linea> lineas = arqueo.findBySesionIdOrderByMetodoPago(sesionId).stream()
                .map(ArqueoMetodo::comoLinea).map(ArqueoPorMetodo.Linea::de).toList();
        return new ResumenDeCierre(fila, cerradoPor, s.getAbiertaEn(), s.getMontoApertura(),
                rubro(suma, "contado"), rubro(suma, "credito"), rubro(suma, "devoluciones").abs(),
                rubro(suma, "recaudos"), rubro(suma, "ingresos"), rubro(suma, "retiros").negate(),
                rubro(suma, "gastos").negate(), rubro(suma, "proveedores").negate(), new ArqueoPorMetodo(lineas),
                s.getObservaciones());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public MovimientosDeCierre movimientosDe(UUID sesionId) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        cierre(negocio, sesionId);
        List<MovimientoDeCaja> movs = movimientos.findBySesionIdOrderByOcurridoEnAsc(sesionId);
        Set<UUID> anulados = movs.stream().map(MovimientoDeCaja::getAnulaA).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<MovimientoEnCierre> filas = movs.stream()
                .map(m -> new MovimientoEnCierre(m.getId(), m.getOcurridoEn(), m.getTipo().name(),
                        nombreDelTipo(m.getTipo()), m.getDocumentoRef(), m.getMetodoPago().name(),
                        nombreDelMetodo(m.getMetodoPago()), conSigno(m), anulados.contains(m.getId())))
                .toList();
        BigDecimal total = filas.stream().map(MovimientoEnCierre::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MovimientosDeCierre(sesionId, filas, total);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public DetalleDeMovimiento movimiento(UUID movimientoId) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        MovimientoDeCaja m = movimientos.findByIdAndNegocioId(movimientoId, negocio)
                .orElseThrow(() -> new NoEncontradoException("Ese movimiento de caja no existe"));
        MovimientoDeCaja anulacion = movimientos.findFirstByAnulaA(m.getId()).orElse(null);

        List<MovimientoDeCaja> delCobro = m.getOrigenId() == null || m.getOrigenTipo() == null ? List.of(m)
                : movimientos.findByNegocioIdAndOrigenTipoAndOrigenIdOrderByOcurridoEnAsc(negocio,
                        m.getOrigenTipo(), m.getOrigenId()).stream()
                        .filter(x -> x.getTipo() == m.getTipo())
                        .toList();
        if (delCobro.isEmpty()) {
            delCobro = List.of(m);
        }

        Set<UUID> usuarios = new HashSet<>();
        usuarios.add(m.getUsuarioId());
        usuarios.add(m.getAutorizadoPor());
        if (anulacion != null) {
            usuarios.add(anulacion.getUsuarioId());
        }
        usuarios.remove(null);
        Map<UUID, String> nombres = usuarios.isEmpty() ? Map.of() : directorio.nombresDe(negocio, usuarios);

        List<PagoDelCobro> pagos = delCobro.stream().map(x -> new PagoDelCobro(x.getId(), x.getMetodoPago().name(),
                nombreDelMetodo(x.getMetodoPago()), x.getMonto(), x.getPropina())).toList();
        boolean deVenta = "VENTA".equals(m.getOrigenTipo()) && m.getOrigenId() != null;
        return new DetalleDeMovimiento(m.getId(), m.getSesionId(), m.getTipo().name(), nombreDelTipo(m.getTipo()),
                m.getDocumentoRef(), m.getOcurridoEn(), m.getMetodoPago().name(), nombreDelMetodo(m.getMetodoPago()),
                conSigno(m), m.getPropina(), m.getConcepto(), nombre(nombres, m.getUsuarioId()),
                nombre(nombres, m.getAutorizadoPor()), anulacion == null ? null : nombre(nombres, anulacion.getUsuarioId()),
                anulacion == null ? null : anulacion.getOcurridoEn(),
                anulacion == null ? m.getMotivoAnulacion() : anulacion.getMotivoAnulacion(), m.getAnulaA(),
                m.getOrigenTipo(), m.getOrigenId(), deVenta ? m.getOrigenId() : null, pagos);
    }

    // ---------------------------------------------------------------------------------------------

    private SesionDeCaja cierre(UUID negocio, UUID sesionId) {
        SesionDeCaja s = sesiones.findByIdAndNegocioId(sesionId, negocio)
                .orElseThrow(() -> new NoEncontradoException("Ese cierre de caja no existe"));
        if (s.getEstado().estaAbierta()) {
            throw new ConflictoDeEstadoException("La sesión sigue abierta: todavía no hay cierre");
        }
        return s;
    }

    /** Arma las filas de C1: caja, cajero y los totales del arqueo por método (HU-140). */
    private List<CierreEnBusqueda> filas(UUID negocio, List<SesionDeCaja> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ArqueoMetodo>> lineas = arqueo
                .findBySesionIdIn(lista.stream().map(SesionDeCaja::getId).toList()).stream()
                .collect(Collectors.groupingBy(ArqueoMetodo::getSesionId));
        Map<UUID, String> nombresDeCaja = new HashMap<>();
        for (UUID cajaId : lista.stream().map(SesionDeCaja::getCajaId).collect(Collectors.toSet())) {
            nombresDeCaja.put(cajaId, cajas.findByIdAndNegocioId(cajaId, negocio).map(Caja::getNombre).orElse(""));
        }
        Set<UUID> cajeros = lista.stream().map(SesionDeCaja::getUsuarioAperturaId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> nombres = cajeros.isEmpty() ? Map.of() : directorio.nombresDe(negocio, cajeros);
        List<CierreEnBusqueda> filas = new ArrayList<>();
        for (SesionDeCaja s : lista) {
            List<ArqueoMetodo> ls = lineas.getOrDefault(s.getId(), List.of());
            // Los cierres anteriores a HU-140 no tienen arqueo por método: queda el del efectivo.
            BigDecimal esperado = ls.isEmpty() ? cero(s.getMontoEsperado())
                    : ls.stream().map(ArqueoMetodo::getEsperado).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal diferencia = ls.isEmpty() ? cero(s.getDiferencia())
                    : ls.stream().map(ArqueoMetodo::getDiferencia).filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            filas.add(new CierreEnBusqueda(s.getId(), s.getCerradaEn(), s.getCajaId(),
                    nombresDeCaja.getOrDefault(s.getCajaId(), ""), s.getNumero(), s.getUsuarioAperturaId(),
                    nombres.getOrDefault(s.getUsuarioAperturaId(), ""), esperado, diferencia, s.getEstado().name()));
        }
        return filas;
    }

    private static BigDecimal conSigno(MovimientoDeCaja m) {
        return m.getMonto().multiply(BigDecimal.valueOf(m.getSigno()));
    }

    private static BigDecimal rubro(Map<String, BigDecimal> suma, String clave) {
        return suma.getOrDefault(clave, BigDecimal.ZERO);
    }

    private static BigDecimal cero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String nombre(Map<UUID, String> nombres, UUID id) {
        return id == null ? null : nombres.getOrDefault(id, "Usuario");
    }

    /** El tipo de movimiento como lo dice la gente (C3, C4). */
    public static String nombreDelTipo(TipoMovimientoCaja tipo) {
        return switch (tipo) {
            case VENTA -> "Venta";
            case COMANDA -> "Comanda";
            case RESERVA -> "Anticipo de reserva";
            case DEVOLUCION -> "Devolución";
            case RECAUDO -> "Recaudo de cartera";
            case INGRESO -> "Ingreso";
            case RETIRO -> "Retiro";
            case GASTO -> "Gasto";
            case AJUSTE -> "Ajuste";
            case APERTURA -> "Base de apertura";
            case ANULACION -> "Anulación";
            case PAGO_PROVEEDOR -> "Pago a proveedor";
        };
    }

    /** El método de pago como lo dice la gente. */
    public static String nombreDelMetodo(MetodoPagoCaja metodo) {
        return switch (metodo) {
            case EFECTIVO -> "Efectivo";
            case TARJETA_DEBITO -> "Tarjeta débito";
            case TARJETA_CREDITO -> "Tarjeta crédito";
            case TRANSFERENCIA -> "Transferencia";
            case QR -> "QR";
            case BONO -> "Bono";
            case CREDITO -> "Crédito";
            case OTRO -> "Otro";
        };
    }
}
