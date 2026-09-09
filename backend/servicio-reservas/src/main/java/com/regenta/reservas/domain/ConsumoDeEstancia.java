package com.regenta.reservas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Un cargo a la habitación (HU-073): el minibar, una cena en el restaurante, la
 * lavandería. Si trae {@code productoId}, al cerrar la estancia se publica el
 * evento que descuenta stock (HU-074). Si viene de una comanda de restaurante,
 * queda enlazado por {@code comandaId}.
 */
public final class ConsumoDeEstancia {

    private final UUID id;
    private final UUID negocioId;
    private final UUID estanciaId;
    private final OrigenConsumo origen;
    private final UUID productoId;
    private final UUID comandaId;
    private final String descripcion;
    private final BigDecimal cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal impuestoPct;
    private final BigDecimal total;
    private final OffsetDateTime cargadoEn;
    private final UUID usuarioId;

    private ConsumoDeEstancia(UUID id, UUID negocioId, UUID estanciaId, OrigenConsumo origen,
            UUID productoId, UUID comandaId, String descripcion, BigDecimal cantidad,
            BigDecimal precioUnitario, BigDecimal impuestoPct, BigDecimal total,
            OffsetDateTime cargadoEn, UUID usuarioId) {
        this.id = id;
        this.negocioId = negocioId;
        this.estanciaId = estanciaId;
        this.origen = origen;
        this.productoId = productoId;
        this.comandaId = comandaId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.impuestoPct = impuestoPct;
        this.total = total;
        this.cargadoEn = cargadoEn;
        this.usuarioId = usuarioId;
    }

    public static ConsumoDeEstancia nuevo(UUID negocioId, UUID estanciaId, OrigenConsumo origen,
            UUID productoId, UUID comandaId, String descripcion, BigDecimal cantidad,
            BigDecimal precioUnitario, BigDecimal impuestoPct, UUID usuarioId) {
        String desc = descripcion == null ? "" : descripcion.trim();
        if (desc.isBlank()) {
            throw new ReglaDeNegocioException("El consumo necesita una descripción");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad del consumo debe ser mayor que cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new ReglaDeNegocioException("El precio del consumo no puede ser negativo");
        }
        BigDecimal impuesto = impuestoPct == null || impuestoPct.signum() < 0 ? BigDecimal.ZERO
                : impuestoPct;
        BigDecimal total = cantidad.multiply(precioUnitario)
                .multiply(BigDecimal.ONE.add(impuesto))
                .setScale(4, RoundingMode.HALF_UP);
        return new ConsumoDeEstancia(UUID.randomUUID(), negocioId, estanciaId,
                origen == null ? OrigenConsumo.OTRO : origen, productoId, comandaId, desc,
                cantidad.setScale(4, RoundingMode.HALF_UP),
                precioUnitario.setScale(4, RoundingMode.HALF_UP),
                impuesto.setScale(4, RoundingMode.HALF_UP), total, null, usuarioId);
    }

    public static ConsumoDeEstancia rehidratar(UUID id, UUID negocioId, UUID estanciaId,
            OrigenConsumo origen, UUID productoId, UUID comandaId, String descripcion,
            BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal impuestoPct,
            BigDecimal total, OffsetDateTime cargadoEn, UUID usuarioId) {
        return new ConsumoDeEstancia(id, negocioId, estanciaId, origen, productoId, comandaId,
                descripcion, cantidad, precioUnitario, impuestoPct, total, cargadoEn, usuarioId);
    }

    public boolean descuentaInventario() {
        return productoId != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getEstanciaId() {
        return estanciaId;
    }

    public OrigenConsumo getOrigen() {
        return origen;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public UUID getComandaId() {
        return comandaId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getImpuestoPct() {
        return impuestoPct;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public OffsetDateTime getCargadoEn() {
        return cargadoEn;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }
}
