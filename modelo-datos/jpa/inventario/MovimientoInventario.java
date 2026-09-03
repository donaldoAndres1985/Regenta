package com.regenta.inventario.dominio;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Libro mayor de inventario: append-only, es la FUENTE DE VERDAD del stock.
 * `Existencia` es solo una proyección que se puede reconstruir sumando esto.
 *
 * Nunca se actualiza ni se borra una fila: un error se corrige con un
 * movimiento contrario, igual que en contabilidad.
 */
@Entity
@Table(name = "movimientos_inventario", schema = "inventario")
@IdClass(MovimientoInventario.Clave.class)
public class MovimientoInventario {

    /** PK compuesta: la tabla está particionada por ocurrido_en y Postgres
     *  exige que toda clave única incluya la columna de partición. */
    @Id @Column(name = "id", updatable = false) private UUID id;
    @Id @Column(name = "ocurrido_en", updatable = false) private OffsetDateTime ocurridoEn;

    @Column(name = "negocio_id",  nullable = false, updatable = false) private UUID negocioId;
    @Column(name = "producto_id", nullable = false, updatable = false) private UUID productoId;
    @Column(name = "bodega_id",   nullable = false, updatable = false) private UUID bodegaId;
    @Column(name = "lote_id", updatable = false) private UUID loteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 25, updatable = false)
    private TipoMovimiento tipo;

    @Column(name = "signo", nullable = false, updatable = false) private short signo;
    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6, updatable = false) private BigDecimal cantidad;
    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4, updatable = false) private BigDecimal costoUnitario;
    @Column(name = "saldo_posterior", nullable = false, precision = 18, scale = 6, updatable = false) private BigDecimal saldoPosterior;

    @Column(name = "origen_tipo", nullable = false, length = 25, updatable = false) private String origenTipo;
    @Column(name = "origen_id", updatable = false) private UUID origenId;
    @Column(name = "usuario_id", updatable = false) private UUID usuarioId;

    /**
     * Clave de idempotencia. RabbitMQ entrega at-least-once: si
     * `venta_completada` llega dos veces, el segundo INSERT choca contra el
     * UNIQUE y el stock no se descuenta dos veces.
     */
    @Column(name = "idempotency_key", nullable = false, length = 120, updatable = false)
    private String idempotencyKey;

    public enum TipoMovimiento {
        ENTRADA_COMPRA, ENTRADA_AJUSTE, ENTRADA_DEVOLUCION, ENTRADA_TRASLADO,
        SALIDA_VENTA, SALIDA_AJUSTE, SALIDA_TRASLADO, SALIDA_MERMA, SALIDA_CONSUMO,
        RESERVA, LIBERACION_RESERVA
    }

    public static class Clave implements java.io.Serializable {
        private UUID id;
        private OffsetDateTime ocurridoEn;
        // equals / hashCode
    }
}
