package com.regenta.reservas.infra;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.regenta.reservas.domain.ConsumoDeEstancia;
import com.regenta.reservas.domain.OrigenConsumo;

/** {@code consumos_estancia} por {@link JdbcTemplate}. La RLS lo acota al negocio de la transacción. */
@Repository
public class RepositorioDeConsumos {

    private static final RowMapper<ConsumoDeEstancia> A_CONSUMO = (rs, fila) ->
            ConsumoDeEstancia.rehidratar(
                    rs.getObject("id", UUID.class),
                    rs.getObject("negocio_id", UUID.class),
                    rs.getObject("estancia_id", UUID.class),
                    OrigenConsumo.valueOf(rs.getString("origen")),
                    rs.getObject("producto_id", UUID.class),
                    rs.getObject("comanda_id", UUID.class),
                    rs.getString("descripcion"),
                    rs.getBigDecimal("cantidad"),
                    rs.getBigDecimal("precio_unitario"),
                    rs.getBigDecimal("impuesto_pct"),
                    rs.getBigDecimal("total"),
                    rs.getObject("cargado_en", OffsetDateTime.class),
                    rs.getObject("usuario_id", UUID.class));

    private final JdbcTemplate jdbc;

    public RepositorioDeConsumos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void agregar(ConsumoDeEstancia c) {
        jdbc.update(
                "INSERT INTO reservas.consumos_estancia "
                        + "(id, negocio_id, estancia_id, origen, producto_id, comanda_id, "
                        + " descripcion, cantidad, precio_unitario, impuesto_pct, total, usuario_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                c.getId(), c.getNegocioId(), c.getEstanciaId(), c.getOrigen().name(),
                c.getProductoId(), c.getComandaId(), c.getDescripcion(), c.getCantidad(),
                c.getPrecioUnitario(), c.getImpuestoPct(), c.getTotal(), c.getUsuarioId());
    }

    public List<ConsumoDeEstancia> porEstancia(UUID estanciaId) {
        return jdbc.query(
                "SELECT id, negocio_id, estancia_id, origen, producto_id, comanda_id, descripcion, "
                        + " cantidad, precio_unitario, impuesto_pct, total, cargado_en, usuario_id "
                        + "FROM reservas.consumos_estancia WHERE estancia_id = ? "
                        + "ORDER BY cargado_en",
                A_CONSUMO, estanciaId);
    }

    public BigDecimal totalDe(UUID estanciaId) {
        BigDecimal suma = jdbc.queryForObject(
                "SELECT coalesce(sum(total), 0) FROM reservas.consumos_estancia "
                        + "WHERE estancia_id = ?",
                BigDecimal.class, estanciaId);
        return suma == null ? BigDecimal.ZERO : suma;
    }
}
