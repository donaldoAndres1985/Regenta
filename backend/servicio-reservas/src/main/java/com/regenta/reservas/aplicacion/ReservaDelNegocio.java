package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.regenta.reservas.domain.Reserva;

/**
 * Una reserva como la ve la recepción (HU-070). Incluye el desglose de la
 * cotización noche por noche para mostrar de dónde sale el total.
 */
public record ReservaDelNegocio(
        UUID id,
        String numero,
        String estado,
        UUID tipoRecursoId,
        UUID recursoId,
        UUID clienteId,
        OffsetDateTime desde,
        OffsetDateTime hasta,
        int noches,
        int numAdultos,
        int numNinos,
        String canal,
        UUID tarifaId,
        UUID politicaCancelacionId,
        BigDecimal subtotal,
        BigDecimal total,
        BigDecimal anticipoRequerido,
        BigDecimal saldo,
        String moneda,
        boolean cotizacionCompleta,
        List<NocheDeReserva> nochesCotizadas) {

    public record NocheDeReserva(java.time.LocalDate fecha, UUID tarifaId, BigDecimal precio) {
    }

    static ReservaDelNegocio de(Reserva r, CotizacionDeEstadia cotizacion) {
        List<NocheDeReserva> detalle = cotizacion == null ? List.of()
                : cotizacion.noches().stream()
                        .map(n -> new NocheDeReserva(n.fecha(), n.tarifaId(), n.precio()))
                        .toList();
        return new ReservaDelNegocio(r.getId(), r.getNumero(), r.getEstado().name(),
                r.getTipoRecursoId(), r.getRecursoId(), r.getClienteId(), r.getDesde(),
                r.getHasta(), r.getNoches(), r.getNumAdultos(), r.getNumNinos(),
                r.getCanal().name(), r.getTarifaId(), r.getPoliticaCancelacionId(),
                r.getSubtotal(), r.getTotal(), r.getAnticipoRequerido(), r.getSaldo(),
                r.getMoneda(), cotizacion == null || cotizacion.completa(), detalle);
    }

    static ReservaDelNegocio de(Reserva r) {
        return de(r, null);
    }
}
