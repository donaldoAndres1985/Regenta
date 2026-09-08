package com.regenta.compras.aplicacion;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.MetodoDePago;
import com.regenta.compras.domain.PagoProveedor;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.infra.CuentaPorPagarRepositorio;
import com.regenta.compras.infra.PagoProveedorRepositorio;
import com.regenta.compras.infra.ProveedorRepositorio;
import com.regenta.compras.infra.RecepcionRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Lo que el negocio le debe a cada proveedor y los pagos que le hace (HU-049).
 *
 * <p>La cuenta nace de una recepción confirmada con la factura del proveedor
 * (criterio 1), su vencimiento sale del plazo de crédito del proveedor. Los
 * pagos bajan el saldo y la dejan {@code PARCIAL} o {@code PAGADA} (criterio 2).
 * El listado va por antigüedad y marca las vencidas (criterio 3). Una factura
 * repetida para el mismo proveedor es 409 (criterio 4).
 */
@Service
public class GestionDeCuentasPorPagar {

    private final CuentaPorPagarRepositorio cuentas;
    private final PagoProveedorRepositorio pagos;
    private final RecepcionRepositorio recepciones;
    private final ProveedorRepositorio proveedores;

    public GestionDeCuentasPorPagar(CuentaPorPagarRepositorio cuentas,
            PagoProveedorRepositorio pagos, RecepcionRepositorio recepciones,
            ProveedorRepositorio proveedores) {
        this.cuentas = cuentas;
        this.pagos = pagos;
        this.recepciones = recepciones;
        this.proveedores = proveedores;
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

    /** Criterio 1: la factura del proveedor sobre una recepción confirmada abre la cuenta. */
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

    /** Criterio 2: el pago baja el saldo y mueve el estado. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public CuentaDelNegocio registrarPago(UUID cuentaId, SolicitudDePago solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        CuentaPorPagar cuenta = delNegocio(cuentaId);

        cuenta.registrarPago(solicitud.monto());
        PagoProveedor pago = PagoProveedor.registrar(negocioId, cuentaId, solicitud.monto(),
                metodoDe(solicitud.metodo()), solicitud.referencia(),
                ContextoDeNegocio.actual().usuario());
        cuentas.save(cuenta);
        pagos.save(pago);

        return CuentaDelNegocio.de(cuenta, LocalDate.now(),
                pagos.findByCuentaIdOrderByFechaAsc(cuentaId));
    }

    private CuentaPorPagar delNegocio(UUID cuentaId) {
        return cuentas.findByIdAndNegocioId(cuentaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa cuenta por pagar no existe"));
    }

    private static MetodoDePago metodoDe(String texto) {
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
