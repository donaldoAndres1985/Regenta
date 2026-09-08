package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;

/** Una recepción con sus líneas y los lotes capturados (HU-048). */
public record RecepcionDelNegocio(
        UUID id,
        String numero,
        UUID ordenId,
        UUID proveedorId,
        UUID bodegaId,
        String estado,
        OffsetDateTime fecha,
        String facturaProveedor,
        BigDecimal total,
        String observaciones,
        List<LineaRecibida> lineas) {

    public record LineaRecibida(
            UUID ordenLineaId,
            UUID productoId,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            String codigoLote,
            LocalDate fechaVencimiento,
            String registroSanitario) {

        static LineaRecibida de(RecepcionLinea l) {
            return new LineaRecibida(l.getOrdenLineaId(), l.getProductoId(), l.getCantidad(),
                    l.getCostoUnitario(), l.getCodigoLote(), l.getFechaVencimiento(),
                    l.getRegistroSanitario());
        }
    }

    static RecepcionDelNegocio de(Recepcion r, List<RecepcionLinea> lineas) {
        return new RecepcionDelNegocio(r.getId(), r.getNumero(), r.getOrdenId(),
                r.getProveedorId(), r.getBodegaId(), r.getEstado().name(), r.getFecha(),
                r.getFacturaProveedor(), r.getTotal(), r.getObservaciones(),
                lineas.stream().map(LineaRecibida::de).toList());
    }
}
