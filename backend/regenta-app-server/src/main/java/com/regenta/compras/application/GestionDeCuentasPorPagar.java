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
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
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

    private final CuentaPorPagarRepositorio cuentas;
    private final PagoProveedorRepositorio pagos;
    private final RecepcionRepositorio recepciones;
    private final ProveedorRepositorio proveedores;
    private final CajaApi caja;
    private final RegistroDeEventos eventos;

    public GestionDeCuentasPorPagar(CuentaPorPagarRepositorio cuentas,
            PagoProveedorRepositorio pagos, RecepcionRepositorio recepciones,
            ProveedorRepositorio proveedores, CajaApi caja, RegistroDeEventos eventos) {
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.recepciones = recepciones;
        this.proveedores = proveedores;
        this.caja = caja;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<CuentaDelNegocio> listar(boolean soloVencidas) {
        LocalDate hoy = LocalDate.now();
        return cuentas.findByNegocioIdOrderByFechaVencimientoAscFechaEmisionAsc(
                        ContextoDeNegocio.negocioActual())
                .stream()
                .filter(c -> !soloVencidas || c.estaVencida(hoy))
                .map(c -> CuentaDelNegocio.de(c, hoy, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public CuentaDelNegocio ver(UUID cuentaId) {
        CuentaPorPagar cuenta = delNegocio(cuentaId);
        return CuentaDelNegocio.de(cuenta, LocalDate.now(),
                pagos.findByCuentaIdOrderByFechaAsc(cuentaId));
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
        if (!cuentas.findByNegocioIdAndRecepcionId(negocioId, recepcion.getId()).isEmpty()) {
            throw new ConflictoDeEstadoException("Esa recepción ya tiene una cuenta por pagar");
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
        return CuentaDelNegocio.de(cuenta, LocalDate.now(),
                pagos.findByCuentaIdOrderByFechaAsc(cuentaId));
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
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID sesion = metodo == MetodoDePago.EFECTIVO ? sesionCajaId : null;
        if (sesion != null && !caja.sesionAbierta(negocioId, sesion)) {
            throw new ConflictoDeEstadoException(
                    "Esa sesión de caja no está abierta: el pago no se registró");
        }
        cuenta.registrarPago(monto);
        PagoProveedor pago = PagoProveedor.registrar(negocioId, cuenta.getId(), monto, metodo,
                referencia, ContextoDeNegocio.actual().usuario(), sesion);
        cuentas.save(cuenta);
        pagos.save(pago);
        eventos.registrar(negocioId, "PagoProveedor", pago.getId(), EVENTO_PAGO,
                payloadDe(negocioId, cuenta, pago));
        return pago;
    }

    private Map<String, Object> payloadDe(UUID negocioId, CuentaPorPagar cuenta, PagoProveedor pago) {
        String proveedor = proveedores.findByIdAndNegocioId(cuenta.getProveedorId(), negocioId)
                .map(p -> p.getNombreComercial() != null ? p.getNombreComercial() : p.getRazonSocial())
                .orElse(null);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("pago_id", pago.getId().toString());
        datos.put("cuenta_id", cuenta.getId().toString());
        datos.put("proveedor_id", cuenta.getProveedorId().toString());
        datos.put("proveedor_nombre", proveedor);
        datos.put("numero_factura", cuenta.getNumeroFactura());
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
