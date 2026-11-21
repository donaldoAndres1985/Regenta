package com.regenta.compras.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.AplicacionDePago;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.RepartoDePago;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.infrastructure.AplicacionDePagoRepositorio;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.NotaCreditoRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * Lo que el negocio le debe a cada proveedor y los pagos que le hace (HU-049, HU-145).
 *
 * <p>La cuenta nace de una recepción confirmada con la factura del proveedor; su vencimiento sale
 * del plazo de crédito del proveedor. Los pagos bajan el saldo y la dejan {@code PARCIAL} o
 * {@code PAGADA}. El listado va por antigüedad y marca las vencidas. Una factura repetida para el
 * mismo proveedor es 409.
 *
 * <p>HU-145 (I29 de {@code docs/dominio/compras.md}): un pago en efectivo puede salir de una sesión
 * de caja; esa sesión tiene que existir y estar abierta, o el pago no se registra (409). Todo pago
 * publica {@code pago_a_proveedor_registrado}; la caja solo registra la salida si fue en efectivo
 * desde una sesión.
 */
@Service
public class GestionDeCuentasPorPagar {

    static final String EVENTO_PAGO = "pago_a_proveedor_registrado";
    static final String EVENTO_ANULACION = "pago_a_proveedor_anulado";

    private final CuentaPorPagarRepositorio cuentas;
    private final PagoProveedorRepositorio pagos;
    private final RecepcionRepositorio recepciones;
    private final ProveedorRepositorio proveedores;
    private final CajaApi caja;
    private final RegistroDeEventos eventos;
    private final AplicacionDePagoRepositorio aplicaciones;
    /** HU-171 C5 / HU-178 · El saldo a favor con cada proveedor. Nulo: no se ofrece usarlo. */
    private final NotaCreditoRepositorio notas;

    public GestionDeCuentasPorPagar(CuentaPorPagarRepositorio cuentas,
            PagoProveedorRepositorio pagos, RecepcionRepositorio recepciones,
            ProveedorRepositorio proveedores, CajaApi caja, RegistroDeEventos eventos,
            AplicacionDePagoRepositorio aplicaciones) {
        this(cuentas, pagos, recepciones, proveedores, caja, eventos, aplicaciones, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GestionDeCuentasPorPagar(CuentaPorPagarRepositorio cuentas,
            PagoProveedorRepositorio pagos, RecepcionRepositorio recepciones,
            ProveedorRepositorio proveedores, CajaApi caja, RegistroDeEventos eventos,
            AplicacionDePagoRepositorio aplicaciones, NotaCreditoRepositorio notas) {
        this.notas = notas;
        this.aplicaciones = aplicaciones;
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.recepciones = recepciones;
        this.proveedores = proveedores;
        this.caja = caja;
        this.eventos = eventos;
    }

    public List<CuentaDelNegocio> listar(boolean soloVencidas) {
        return listar(soloVencidas, false);
    }

    /**
     * Por antigüedad (vencimiento más viejo primero). HU-170 C4: {@code soloEnEsperaDeFactura} deja
     * solo las que llegaron con remisión, con el número de su recepción.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<CuentaDelNegocio> listar(boolean soloVencidas, boolean soloEnEsperaDeFactura) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        LocalDate hoy = LocalDate.now();
        List<CuentaPorPagar> filas = cuentas
                .findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId).stream()
                .filter(c -> !soloVencidas || c.estaVencida(hoy))
                .filter(c -> !soloEnEsperaDeFactura || c.enEsperaDeFactura())
                .toList();
        Map<UUID, String> nombres = nombresDeProveedores(filas);
        Map<UUID, String> numeros = numerosDeRecepciones(filas);
        return filas.stream()
                .map(c -> CuentaDelNegocio.de(c, hoy, List.of(), nombres.get(c.getProveedorId()),
                        c.getRecepcionId() == null ? null : numeros.get(c.getRecepcionId())))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public CuentaDelNegocio ver(UUID cuentaId) {
        return vista(delNegocio(cuentaId));
    }

    /**
     * HU-170 C3 · Llegó la factura de una cuenta en espera: se quita la marca y el vencimiento se
     * recalcula desde la fecha de la factura con los días de crédito del proveedor. El número no se
     * repite para el mismo proveedor (409).
     */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public CuentaDelNegocio registrarFacturaDeCuenta(UUID cuentaId, SolicitudDeFacturaDeCuenta solicitud) {
        CuentaPorPagar cuenta = delNegocio(cuentaId);
        completarFactura(cuenta, solicitud.numeroFactura(), solicitud.fechaFactura());
        return vista(cuenta);
    }

    /** HU-049 C1: la factura del proveedor sobre una recepción confirmada abre la cuenta. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public CuentaDelNegocio registrarFactura(SolicitudDeFacturaDeRecepcion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String numeroFactura = solicitud.numeroFactura().trim();
        Recepcion recepcion = recepciones
                .findByIdAndNegocioId(solicitud.recepcionId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa recepción no existe"));
        if (recepcion.getEstado() != EstadoRecepcion.CONFIRMADA) {
            throw new ConflictoDeEstadoException("La recepción todavía no está confirmada");
        }
        List<CuentaPorPagar> existentes = cuentas.findByNegocioIdAndRecepcionId(negocioId, recepcion.getId());
        if (!existentes.isEmpty()) {
            // HU-170 · Si llegó con remisión, su cuenta espera esta factura: se completa.
            CuentaPorPagar enEspera = existentes.stream().filter(CuentaPorPagar::enEsperaDeFactura)
                    .findFirst()
                    .orElseThrow(() -> new ConflictoDeEstadoException("Esa recepción ya tiene una cuenta por pagar"));
            completarFactura(enEspera, numeroFactura,
                    solicitud.fechaFactura() == null ? LocalDate.now() : solicitud.fechaFactura());
            return vista(enEspera);
        }
        if (recepcion.getTotal().signum() <= 0) {
            throw new ReglaDeNegocioException("La recepción no tiene monto para facturar");
        }
        if (cuentas.existsByNegocioIdAndProveedorIdAndNumeroFactura(
                negocioId, recepcion.getProveedorId(), numeroFactura)) {
            throw new RecursoDuplicadoException(
                    "Ya hay una cuenta con la factura " + numeroFactura + " de ese proveedor");
        }
        Proveedor proveedor = proveedores
                .findByIdAndNegocioId(recepcion.getProveedorId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));

        CuentaPorPagar cuenta = CuentaPorPagar.abrirPorRecepcion(negocioId,
                recepcion.getProveedorId(), recepcion.getId(), numeroFactura,
                recepcion.getTotal().setScale(2, RoundingMode.HALF_UP), LocalDate.now(),
                proveedor.getDiasCredito());
        recepcion.registrarFactura(numeroFactura);
        recepciones.save(recepcion);
        try {
            cuentas.save(cuenta);
            cuentas.flush();
        } catch (DataIntegrityViolationException duplicada) {
            throw new RecursoDuplicadoException(
                    "Ya hay una cuenta con la factura " + numeroFactura + " de ese proveedor");
        }
        return CuentaDelNegocio.de(cuenta, LocalDate.now(), List.of());
    }

    /** HU-049 C2 y HU-145: el pago baja el saldo y, si salió de la caja, la caja lo registra. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public CuentaDelNegocio registrarPago(UUID cuentaId, SolicitudDePago solicitud) {
        CuentaPorPagar cuenta = delNegocio(cuentaId);
        pagar(cuenta, solicitud.monto(), metodoDe(solicitud.metodo()), solicitud.referencia(),
                solicitud.sesionCajaId());
        return vista(cuenta);
    }

    /**
     * Abona {@code monto} a la cuenta y publica el pago. Lo usa también la compra de contado
     * (HU-169), en la misma transacción que la confirma.
     *
     * @throws ConflictoDeEstadoException si el pago es en efectivo desde una sesión de caja que no
     *         existe o no está abierta (HU-145 C4): no se registra nada.
     */
    PagoProveedor pagar(CuentaPorPagar cuenta, BigDecimal monto, MetodoDePago metodo,
            String referencia, UUID sesionCajaId) {
        return registrarPagoA(cuenta.getProveedorId(), monto, metodo, referencia, sesionCajaId,
                List.of(new RepartoDePago.Abono(cuenta, monto)), BigDecimal.ZERO);
    }

    /**
     * HU-171 · Un pago a un proveedor que se reparte entre sus cuentas abiertas: por omisión de la más
     * antigua a la más nueva, o como lo indique el usuario. Nunca más de lo que se debe; una cuenta de
     * otro proveedor en el reparto es 422. En efectivo con la plata de la caja sale de la sesión
     * abierta del usuario (sin ella, 409).
     */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public PagoDelNegocio pagarAProveedor(SolicitudDePagoAProveedor solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Proveedor proveedor = proveedores.findByIdAndNegocioId(solicitud.proveedorId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));
        List<CuentaPorPagar> abiertas = abiertasDe(negocioId, proveedor.getId());
        Map<UUID, BigDecimal> indicado = null;
        if (solicitud.aplicaciones() != null && !solicitud.aplicaciones().isEmpty()) {
            indicado = new LinkedHashMap<>();
            for (SolicitudDePagoAProveedor.Aplicacion a : solicitud.aplicaciones()) {
                indicado.merge(a.cuentaId(), a.monto(), BigDecimal::add);
            }
        }
        // HU-171 C5 · Con saldo a favor, se reparte el monto más lo que se use de él, sin pasar la deuda.
        BigDecimal usado = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(solicitud.usarSaldoAFavor()) && notas != null) {
            BigDecimal deuda = abiertas.stream().map(CuentaPorPagar::getSaldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            usado = notas.saldoAFavorDe(negocioId, proveedor.getId())
                    .min(deuda.subtract(solicitud.monto())).max(BigDecimal.ZERO);
        }
        List<RepartoDePago.Abono> abonos = RepartoDePago.repartir(solicitud.monto().add(usado), abiertas, indicado);
        MetodoDePago metodo = metodoDe(solicitud.metodo());
        UUID sesion = null;
        if (metodo == MetodoDePago.EFECTIVO && Boolean.TRUE.equals(solicitud.pagarDesdeLaCaja())) {
            UUID usuario = ContextoDeNegocio.actual().usuario();
            sesion = caja.sesionAbiertaDe(negocioId, usuario).orElseThrow(() -> new ConflictoDeEstadoException(
                    "No tienes una caja abierta: ábrela o marca que pagaste con dinero de fuera de la caja"));
        }
        PagoProveedor pago = registrarPagoA(proveedor.getId(), solicitud.monto(), metodo,
                solicitud.referencia(), sesion, abonos, usado);
        consumirSaldoAFavor(negocioId, proveedor.getId(), usado);
        return PagoDelNegocio.de(pago, abonos);
    }

    /**
     * HU-172 · Anula un pago registrado por error (I28): lo que abonó vuelve al saldo de cada cuenta y
     * su estado se recalcula; el pago queda visible con motivo, usuario y fecha. Si salió de la caja,
     * Caja recibe el reverso: en la misma sesión si sigue abierta, o en la abierta que indique el
     * usuario (o la suya); si no hay ninguna, 409 y no se anula.
     */
    @Transactional
    @RequierePermiso("COMPRAS_PAGO_ANULAR")
    public PagoDelNegocio anularPago(UUID pagoId, SolicitudDeAnulacionDePago solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        PagoProveedor pago = pagos.findById(pagoId)
                .filter(p -> negocioId.equals(p.getNegocioId()))
                .orElseThrow(() -> new NoEncontradoException("Ese pago no existe"));
        if (pago.estaAnulado()) {
            throw new ConflictoDeEstadoException("Ese pago ya está anulado");
        }
        UUID sesionReverso = pago.salioDeLaCaja() ? sesionParaElReverso(negocioId, pago, solicitud) : null;

        List<AplicacionDePago> deEstePago = aplicaciones.findByPagoId(pagoId);
        List<RepartoDePago.Abono> revertidos = new java.util.ArrayList<>();
        for (AplicacionDePago a : deEstePago) {
            CuentaPorPagar cuenta = cuentas.findByIdAndNegocioId(a.getCuentaId(), negocioId)
                    .orElseThrow(() -> new NoEncontradoException("Una cuenta de ese pago no existe"));
            cuenta.revertirPago(a.getMonto());
            cuentas.save(cuenta);
            revertidos.add(new RepartoDePago.Abono(cuenta, a.getMonto()));
        }
        pago.anular(ContextoDeNegocio.actual().usuario(), solicitud.motivo(), sesionReverso);
        pagos.save(pago);
        devolverSaldoAFavor(negocioId, pago.getProveedorId(), pago.getSaldoAFavorUsado());

        Map<String, Object> datos = payloadDe(negocioId, pago, revertidos);
        datos.put("sesion_reverso_id", sesionReverso == null ? null : sesionReverso.toString());
        datos.put("motivo", pago.getMotivoAnulacion());
        eventos.registrar(negocioId, "PagoProveedor", pago.getId(), EVENTO_ANULACION, datos);
        return PagoDelNegocio.de(pago, revertidos);
    }

    /** HU-172 · Un pago con lo que abonó a cada cuenta, anulado o no. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public PagoDelNegocio verPago(UUID pagoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        PagoProveedor pago = pagos.findById(pagoId)
                .filter(p -> negocioId.equals(p.getNegocioId()))
                .orElseThrow(() -> new NoEncontradoException("Ese pago no existe"));
        List<RepartoDePago.Abono> abonos = aplicaciones.findByPagoId(pagoId).stream()
                .map(a -> cuentas.findByIdAndNegocioId(a.getCuentaId(), negocioId)
                        .map(c -> new RepartoDePago.Abono(c, a.getMonto())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return PagoDelNegocio.de(pago, abonos);
    }

    /**
     * HU-172 C3 / C4 · La sesión que recibe el reverso: la misma si sigue abierta; si ya cerró, la
     * abierta que indicó el usuario, o la suya.
     */
    private UUID sesionParaElReverso(UUID negocioId, PagoProveedor pago, SolicitudDeAnulacionDePago solicitud) {
        if (caja.sesionAbierta(negocioId, pago.getSesionCajaId())) {
            return pago.getSesionCajaId();
        }
        if (solicitud.sesionCajaId() != null) {
            if (!caja.sesionAbierta(negocioId, solicitud.sesionCajaId())) {
                throw new ConflictoDeEstadoException(
                        "Esa sesión de caja no está abierta: el pago no se anuló");
            }
            return solicitud.sesionCajaId();
        }
        UUID usuario = ContextoDeNegocio.actual().usuario();
        return caja.sesionAbiertaDe(negocioId, usuario).orElseThrow(() -> new ConflictoDeEstadoException(
                "La caja de la que salió el pago ya cerró y no tienes una abierta para devolver el efectivo: "
                        + "abre tu caja e intenta de nuevo"));
    }

    /** Las cuentas del proveedor con saldo, de la más antigua (vence primero) a la más nueva. */
    private List<CuentaPorPagar> abiertasDe(UUID negocioId, UUID proveedorId) {
        return cuentas.findByNegocioIdAndProveedorIdOrderByFechaVencimientoAscFechaEmisionAsc(negocioId,
                        proveedorId).stream()
                .filter(c -> c.getSaldo().signum() > 0)
                .filter(c -> c.getEstado() != EstadoCuentaPorPagar.PAGADA
                        && c.getEstado() != EstadoCuentaPorPagar.ANULADA)
                .toList();
    }

    private PagoProveedor registrarPagoA(UUID proveedorId, BigDecimal monto, MetodoDePago metodo,
            String referencia, UUID sesionCajaId, List<RepartoDePago.Abono> abonos, BigDecimal saldoAFavorUsado) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID sesion = metodo == MetodoDePago.EFECTIVO ? sesionCajaId : null;
        if (sesion != null && !caja.sesionAbierta(negocioId, sesion)) {
            throw new ConflictoDeEstadoException(
                    "Esa sesión de caja no está abierta: el pago no se registró");
        }
        for (RepartoDePago.Abono abono : abonos) {
            abono.cuenta().registrarPago(abono.monto());
            cuentas.save(abono.cuenta());
        }
        UUID unaSolaCuenta = abonos.size() == 1 ? abonos.get(0).cuenta().getId() : null;
        PagoProveedor pago = PagoProveedor.registrar(negocioId, proveedorId, unaSolaCuenta, monto, metodo,
                referencia, ContextoDeNegocio.actual().usuario(), sesion);
        pago.usoSaldoAFavor(saldoAFavorUsado);
        pagos.save(pago);
        aplicaciones.saveAll(abonos.stream()
                .map(a -> AplicacionDePago.de(negocioId, pago.getId(), a.cuenta().getId(), a.monto()))
                .toList());
        eventos.registrar(negocioId, "PagoProveedor", pago.getId(), EVENTO_PAGO,
                payloadDe(negocioId, pago, abonos));
        return pago;
    }

    /** HU-171 C5 · Gasta el saldo a favor de las notas más antiguas primero. */
    private void consumirSaldoAFavor(UUID negocioId, UUID proveedorId, BigDecimal usado) {
        if (notas == null || usado.signum() <= 0) {
            return;
        }
        BigDecimal falta = usado;
        for (var nota : notas.findByNegocioIdAndProveedorIdOrderByCreadoEnAsc(negocioId, proveedorId)) {
            if (falta.signum() <= 0) {
                break;
            }
            BigDecimal gastado = nota.usar(falta);
            if (gastado.signum() > 0) {
                falta = falta.subtract(gastado);
                notas.save(nota);
            }
        }
    }

    /** HU-172 · Se anuló un pago que usó saldo a favor: vuelve a las notas. */
    private void devolverSaldoAFavor(UUID negocioId, UUID proveedorId, BigDecimal usado) {
        if (notas == null || usado == null || usado.signum() <= 0) {
            return;
        }
        BigDecimal falta = usado;
        for (var nota : notas.findByNegocioIdAndProveedorIdOrderByCreadoEnAsc(negocioId, proveedorId)) {
            if (falta.signum() <= 0) {
                break;
            }
            BigDecimal recuperado = nota.recuperar(falta);
            if (recuperado.signum() > 0) {
                falta = falta.subtract(recuperado);
                notas.save(nota);
            }
        }
    }

    private void completarFactura(CuentaPorPagar cuenta, String numero, LocalDate fecha) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String limpio = numero == null ? "" : numero.trim();
        if (!limpio.isEmpty() && cuentas.existsByNegocioIdAndProveedorIdAndNumeroFactura(
                negocioId, cuenta.getProveedorId(), limpio)) {
            throw new RecursoDuplicadoException(
                    "Ya hay una cuenta con la factura " + limpio + " de ese proveedor");
        }
        Proveedor proveedor = proveedores.findByIdAndNegocioId(cuenta.getProveedorId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));
        cuenta.registrarFactura(limpio, fecha, proveedor.getDiasCredito());
        if (cuenta.getRecepcionId() != null) {
            recepciones.findByIdAndNegocioId(cuenta.getRecepcionId(), negocioId).ifPresent(r -> {
                if (!r.tieneFactura()) {
                    r.facturaRecibida(limpio, fecha);
                    recepciones.save(r);
                }
            });
        }
        try {
            cuentas.save(cuenta);
            cuentas.flush();
        } catch (DataIntegrityViolationException duplicada) {
            throw new RecursoDuplicadoException(
                    "Ya hay una cuenta con la factura " + limpio + " de ese proveedor");
        }
    }

    private CuentaDelNegocio vista(CuentaPorPagar cuenta) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String proveedor = proveedores.findByIdAndNegocioId(cuenta.getProveedorId(), negocioId)
                .map(GestionDeCuentasPorPagar::nombreDe).orElse(null);
        String recepcion = cuenta.getRecepcionId() == null ? null
                : recepciones.findByIdAndNegocioId(cuenta.getRecepcionId(), negocioId)
                        .map(Recepcion::getNumero).orElse(null);
        List<AplicacionDePago> deLaCuenta = aplicaciones.findByCuentaId(cuenta.getId());
        Map<UUID, PagoProveedor> porId = new java.util.HashMap<>();
        pagos.findAllById(deLaCuenta.stream().map(AplicacionDePago::getPagoId).distinct().toList())
                .forEach(p -> porId.put(p.getId(), p));
        List<CuentaDelNegocio.PagoRegistrado> registrados = deLaCuenta.stream()
                .filter(a -> porId.containsKey(a.getPagoId()))
                .map(a -> CuentaDelNegocio.PagoRegistrado.de(porId.get(a.getPagoId()), a.getMonto()))
                .sorted(java.util.Comparator.comparing(CuentaDelNegocio.PagoRegistrado::fecha))
                .toList();
        return CuentaDelNegocio.de(cuenta, LocalDate.now(), registrados, proveedor, recepcion);
    }

    private Map<UUID, String> nombresDeProveedores(List<CuentaPorPagar> filas) {
        Map<UUID, String> nombres = new java.util.HashMap<>();
        proveedores.findAllById(filas.stream().map(CuentaPorPagar::getProveedorId).distinct().toList())
                .forEach(p -> nombres.put(p.getId(), nombreDe(p)));
        return nombres;
    }

    private Map<UUID, String> numerosDeRecepciones(List<CuentaPorPagar> filas) {
        Map<UUID, String> numeros = new java.util.HashMap<>();
        recepciones.findAllById(filas.stream().map(CuentaPorPagar::getRecepcionId)
                        .filter(java.util.Objects::nonNull).distinct().toList())
                .forEach(r -> numeros.put(r.getId(), r.getNumero()));
        return numeros;
    }

    private static String documentoDe(List<RepartoDePago.Abono> abonos) {
        if (abonos.isEmpty()) {
            return null;
        }
        String primera = abonos.get(0).cuenta().getNumeroFactura();
        String texto = primera == null ? "Remisión" : primera;
        if (abonos.size() > 1) {
            texto = texto + " y " + (abonos.size() - 1) + " más";
        }
        return texto.length() > 40 ? texto.substring(0, 40) : texto;
    }

    private static String nombreDe(Proveedor p) {
        return p.getNombreComercial() != null ? p.getNombreComercial() : p.getRazonSocial();
    }

    private Map<String, Object> payloadDe(UUID negocioId, PagoProveedor pago,
            List<RepartoDePago.Abono> abonos) {
        String proveedor = proveedores.findByIdAndNegocioId(pago.getProveedorId(), negocioId)
                .map(GestionDeCuentasPorPagar::nombreDe)
                .orElse(null);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("pago_id", pago.getId().toString());
        datos.put("cuenta_id", abonos.size() == 1 ? abonos.get(0).cuenta().getId().toString() : null);
        datos.put("proveedor_id", pago.getProveedorId().toString());
        datos.put("proveedor_nombre", proveedor);
        // El documento que verá la caja: la factura, o la primera y cuántas más (hasta 40 caracteres).
        datos.put("numero_factura", documentoDe(abonos));
        List<Map<String, Object>> partes = new java.util.ArrayList<>();
        for (RepartoDePago.Abono a : abonos) {
            Map<String, Object> parte = new LinkedHashMap<>();
            parte.put("cuenta_id", a.cuenta().getId().toString());
            parte.put("numero_factura", a.cuenta().getNumeroFactura());
            parte.put("monto", a.monto());
            partes.add(parte);
        }
        datos.put("aplicaciones", partes);
        datos.put("monto", pago.getMonto());
        datos.put("metodo", pago.getMetodo().name());
        datos.put("sesion_caja_id", pago.getSesionCajaId() == null ? null : pago.getSesionCajaId().toString());
        UUID usuario = ContextoDeNegocio.actual().usuario();
        datos.put("usuario_id", usuario == null ? null : usuario.toString());
        return datos;
    }

    private CuentaPorPagar delNegocio(UUID cuentaId) {
        return cuentas.findByIdAndNegocioId(cuentaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa cuenta por pagar no existe"));
    }

    static MetodoDePago metodoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return MetodoDePago.OTRO;
        }
        try {
            return MetodoDePago.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Método de pago no válido: " + texto);
        }
    }
}
