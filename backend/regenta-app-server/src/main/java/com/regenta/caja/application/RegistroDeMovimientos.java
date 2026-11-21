package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;

/**
 * Los cobros de los tres patrones entran a la caja (HU-060). Un evento de cobro
 * (venta, comanda, reserva) que trae la sesión de caja y sus pagos se traduce a
 * uno o varios {@code movimientos_caja}. El {@code idempotency_key} determinista
 * más {@code uq_mov_caja_idem} impiden el doble registro si el evento llega dos
 * veces (criterio 4), además del Inbox del consumidor.
 */
@Service
public class RegistroDeMovimientos {

    private static final Logger log = LoggerFactory.getLogger(RegistroDeMovimientos.class);

    private final MovimientoDeCajaRepositorio movimientos;
    private final SesionDeCajaRepositorio sesiones;

    public RegistroDeMovimientos(MovimientoDeCajaRepositorio movimientos,
            SesionDeCajaRepositorio sesiones) {
        this.movimientos = movimientos;
        this.sesiones = sesiones;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_MOVIMIENTO_VER")
    public List<MovimientoDelNegocio> deLaSesion(UUID sesionId) {
        sesiones.findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
        return movimientos.findBySesionIdOrderByOcurridoEnAsc(sesionId).stream()
                .map(MovimientoDelNegocio::de).toList();
    }

    /** Traduce un evento de cobro a movimientos. No exige permiso: lo llama el consumidor. */
    @Transactional
    public int registrarDesdeCobro(String origenTipo, Map<String, Object> datos) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID sesionId = uuid(datos.get("sesion_caja_id"));
        if (sesionId == null) {
            return 0; // el cobro no pasó por una sesión de caja
        }
        SesionDeCaja sesion = sesiones.findByIdAndNegocioId(sesionId, negocioId).orElse(null);
        if (sesion == null || !sesion.getEstado().estaAbierta()) {
            log.debug("Cobro para la sesión {} que no está abierta: se ignora", sesionId);
            return 0;
        }

        TipoMovimientoCaja tipo = TipoMovimientoCaja.valueOf(origenTipo);
        UUID origenId = primerUuid(datos, "origen_id", "venta_id", "pedido_id", "reserva_id");
        String documentoRef = texto(datos.get("numero"));
        UUID usuarioEvento = uuid(datos.get("usuario_id"));
        UUID usuarioId = usuarioEvento != null ? usuarioEvento : sesion.getUsuarioAperturaId();

        List<Map<String, Object>> pagos = pagosDe(datos);
        int registrados = 0;
        for (int i = 0; i < pagos.size(); i++) {
            Map<String, Object> pago = pagos.get(i);
            BigDecimal monto = numero(pago.get("monto"));
            if (monto == null || monto.signum() <= 0) {
                continue;
            }
            String idem = origenTipo + ":" + origenId + ":pago:" + i;
            if (movimientos.existsByNegocioIdAndIdempotencyKey(negocioId, idem)) {
                continue;
            }
            MovimientoDeCaja mov = MovimientoDeCaja.deCobro(negocioId, sesionId, usuarioId, tipo,
                    origenTipo, origenId, documentoRef, "Cobro " + origenTipo.toLowerCase(),
                    MetodoPagoCaja.desde(texto(pago.get("metodo"))), monto,
                    numero(pago.get("propina")), idem);
            try {
                movimientos.save(mov);
                movimientos.flush();
                registrados++;
            } catch (DataIntegrityViolationException repetido) {
                log.debug("Movimiento {} ya registrado: se ignora", idem);
            }
        }
        return registrados;
    }

    /**
     * HU-145 · Un pago a proveedor en efectivo hecho con la plata de la caja: una salida
     * {@code PAGO_PROVEEDOR} por el mismo monto, con el número de factura como documento (C1). Si el
     * pago no fue en efectivo (C2) o no indicó sesión (C3), la caja no cambia. Compras ya validó
     * que la sesión estaba abierta (C4); si se cerró entretanto, se ignora, como con los cobros.
     * El mismo pago dos veces no duplica la salida (C5).
     */
    @Transactional
    public boolean registrarPagoAProveedor(Map<String, Object> datos) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID sesionId = uuid(datos.get("sesion_caja_id"));
        if (sesionId == null || !"EFECTIVO".equalsIgnoreCase(texto(datos.get("metodo")))) {
            return false;
        }
        SesionDeCaja sesion = sesiones.findByIdAndNegocioId(sesionId, negocioId).orElse(null);
        if (sesion == null || !sesion.getEstado().estaAbierta()) {
            log.warn("Pago a proveedor para la sesión {} que ya no está abierta: no entra a la caja",
                    sesionId);
            return false;
        }
        BigDecimal monto = numero(datos.get("monto"));
        UUID pagoId = uuid(datos.get("pago_id"));
        if (monto == null || monto.signum() <= 0 || pagoId == null) {
            return false;
        }
        String idem = "PAGO_PROVEEDOR:" + pagoId;
        if (movimientos.existsByNegocioIdAndIdempotencyKey(negocioId, idem)) {
            return false;
        }
        UUID usuarioEvento = uuid(datos.get("usuario_id"));
        String proveedor = texto(datos.get("proveedor_nombre"));
        String concepto = "Pago a proveedor" + (proveedor == null ? "" : " " + proveedor);
        MovimientoDeCaja mov = MovimientoDeCaja.pagoAProveedor(negocioId, sesionId,
                usuarioEvento != null ? usuarioEvento : sesion.getUsuarioAperturaId(), pagoId,
                recortar(texto(datos.get("numero_factura")), 40),
                concepto.length() > 200 ? concepto.substring(0, 200) : concepto, monto, idem);
        try {
            movimientos.save(mov);
            movimientos.flush();
            return true;
        } catch (DataIntegrityViolationException repetido) {
            log.debug("Pago a proveedor {} ya registrado en la caja: se ignora", idem);
            return false;
        }
    }

    /**
     * HU-172 · Se anuló un pago a proveedor que había salido de la caja: entra el reverso por el mismo
     * monto en la sesión que eligió Compras (C3, C4). Si la sesión ya no está abierta, si la salida
     * original no existe o ya se reversó, no se registra nada: el mismo aviso dos veces no duplica.
     */
    @Transactional
    public boolean reversarPagoAProveedor(Map<String, Object> datos) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID pagoId = uuid(datos.get("pago_id"));
        UUID sesionId = uuid(datos.get("sesion_reverso_id"));
        if (pagoId == null || sesionId == null) {
            return false;
        }
        SesionDeCaja sesion = sesiones.findByIdAndNegocioId(sesionId, negocioId).orElse(null);
        if (sesion == null || !sesion.getEstado().estaAbierta()) {
            log.warn("Reverso del pago a proveedor {} para la sesión {} que ya no está abierta", pagoId, sesionId);
            return false;
        }
        MovimientoDeCaja salida = movimientos
                .findByNegocioIdAndIdempotencyKey(negocioId, "PAGO_PROVEEDOR:" + pagoId).orElse(null);
        if (salida == null || movimientos.existsByAnulaA(salida.getId())) {
            return false;
        }
        UUID usuarioEvento = uuid(datos.get("usuario_id"));
        MovimientoDeCaja reverso = MovimientoDeCaja.reversoDePagoAProveedor(salida, sesionId,
                usuarioEvento != null ? usuarioEvento : sesion.getUsuarioAperturaId(),
                texto(datos.get("motivo")), "PAGO_PROVEEDOR:" + pagoId + ":ANULACION");
        try {
            movimientos.save(reverso);
            movimientos.flush();
            return true;
        } catch (DataIntegrityViolationException repetido) {
            log.debug("Reverso del pago a proveedor {} ya registrado: se ignora", pagoId);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> pagosDe(Map<String, Object> datos) {
        Object crudo = datos.get("pagos");
        if (crudo instanceof List<?> lista && !lista.isEmpty()) {
            List<Map<String, Object>> pagos = new ArrayList<>();
            for (Object e : lista) {
                if (e instanceof Map) {
                    pagos.add((Map<String, Object>) e);
                }
            }
            return pagos;
        }
        // Pago único plano.
        Object monto = datos.get("monto");
        if (monto == null) {
            return List.of();
        }
        return List.of(Map.of("metodo",
                datos.getOrDefault("metodo_pago", datos.getOrDefault("metodo", "OTRO")),
                "monto", monto,
                "propina", datos.getOrDefault("propina", 0)));
    }

    private static UUID primerUuid(Map<String, Object> datos, String... claves) {
        for (String c : claves) {
            UUID v = uuid(datos.get(c));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static UUID uuid(Object v) {
        try {
            return v == null ? null : UUID.fromString(v.toString());
        } catch (IllegalArgumentException noEsUuid) {
            return null;
        }
    }

    private static BigDecimal numero(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException noEsNumero) {
            return null;
        }
    }

    private static String recortar(String v, int largo) {
        return v == null || v.length() <= largo ? v : v.substring(0, largo);
    }

    private static String texto(Object v) {
        return v == null || v.toString().isBlank() ? null : v.toString();
    }
}
