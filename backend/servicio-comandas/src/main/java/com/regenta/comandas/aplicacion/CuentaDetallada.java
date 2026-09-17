package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.comandas.domain.ComandaLinea;
import com.regenta.comandas.domain.Cuenta;
import com.regenta.comandas.domain.CuentaLinea;

/** Una cuenta con su reparto de líneas (HU-089). */
public record CuentaDetallada(
        UUID id,
        UUID comandaId,
        int numeroDivision,
        String etiqueta,
        String modoDivision,
        BigDecimal subtotal,
        BigDecimal impuestoTotal,
        BigDecimal propina,
        BigDecimal propinaSugerida,
        BigDecimal total,
        BigDecimal pagado,
        String estado,
        List<LineaDeCuenta> lineas) {

    public record LineaDeCuenta(UUID lineaId, String nombre, BigDecimal proporcion, BigDecimal monto) {
    }

    static CuentaDetallada de(Cuenta c, List<CuentaLinea> propias, Map<UUID, ComandaLinea> comandaLineasPorId) {
        List<LineaDeCuenta> lineas = propias.stream()
                .map(cl -> {
                    ComandaLinea l = comandaLineasPorId.get(cl.getComandaLineaId());
                    return new LineaDeCuenta(cl.getComandaLineaId(), l == null ? null : l.getNombreSnapshot(),
                            cl.getProporcion(), cl.getMonto());
                })
                .toList();
        return new CuentaDetallada(c.getId(), c.getComandaId(), c.getNumeroDivision(), c.getEtiqueta(),
                c.getModoDivision().name(), c.getSubtotal(), c.getImpuestoTotal(), c.getPropina(),
                c.propinaSugerida(), c.getTotal(), c.getPagado(), c.getEstado().name(), lineas);
    }
}
