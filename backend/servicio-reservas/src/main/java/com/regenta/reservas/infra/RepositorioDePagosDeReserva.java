package com.regenta.reservas.infra;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.regenta.reservas.domain.PagoDeReserva;

/** {@code pagos_reserva} por {@link JdbcTemplate}. La RLS lo acota al negocio de la transacción. */
@Repository
public class RepositorioDePagosDeReserva {

    private final JdbcTemplate jdbc;

    public RepositorioDePagosDeReserva(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrar(PagoDeReserva p) {
        jdbc.update(
                "INSERT INTO reservas.pagos_reserva "
                        + "(id, negocio_id, reserva_id, tipo, metodo, monto, referencia, "
                        + " caja_sesion_id, usuario_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.getId(), p.getNegocioId(), p.getReservaId(), p.getTipo().name(),
                p.getMetodo().name(), p.getMonto(), p.getReferencia(), p.getCajaSesionId(),
                p.getUsuarioId());
    }

    /** Lo abonado a una reserva: la suma de los pagos que cuentan como pago del huésped. */
    public BigDecimal abonadoA(UUID reservaId) {
        BigDecimal suma = jdbc.queryForObject(
                "SELECT coalesce(sum(monto), 0) FROM reservas.pagos_reserva "
                        + "WHERE reserva_id = ? AND tipo IN ('ANTICIPO','SALDO','DEPOSITO','CONSUMO')",
                BigDecimal.class, reservaId);
        return suma == null ? BigDecimal.ZERO : suma;
    }
}
