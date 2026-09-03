package com.regenta.ventas.dominio;

import com.regenta.comun.dominio.EntidadTenant;
import com.regenta.comun.mensajeria.OutboxEvento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agregado raíz del patrón Venta directa.
 *
 * Frontera del agregado: Venta + VentaLinea + PagoVenta se guardan juntos y
 * son consistentes entre sí. Todo lo demás (producto, cliente, factura) es
 * un UUID: vive en otro servicio y otra base.
 */
@Entity
@Table(name = "ventas", schema = "ventas")
public class Venta extends EntidadTenant {

    @Column(name = "numero", nullable = false, length = 30) private String numero;
    @Column(name = "sucursal_id") private UUID sucursalId;
    @Column(name = "bodega_id", nullable = false) private UUID bodegaId;

    /** Referencias lógicas: no hay @ManyToOne hacia otros servicios. */
    @Column(name = "cliente_id") private UUID clienteId;
    @Column(name = "usuario_id", nullable = false) private UUID usuarioId;
    @Column(name = "caja_sesion_id") private UUID cajaSesionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 25)
    private EstadoVenta estado = EstadoVenta.BORRADOR;

    @Column(name = "fecha", nullable = false) private OffsetDateTime fecha = OffsetDateTime.now();

    @Column(name = "subtotal",        nullable = false, precision = 16, scale = 4) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "descuento_total", nullable = false, precision = 16, scale = 4) private BigDecimal descuentoTotal = BigDecimal.ZERO;
    @Column(name = "impuesto_total",  nullable = false, precision = 16, scale = 4) private BigDecimal impuestoTotal = BigDecimal.ZERO;
    @Column(name = "total",           nullable = false, precision = 16, scale = 4) private BigDecimal total = BigDecimal.ZERO;
    @Column(name = "costo_total",     nullable = false, precision = 16, scale = 4) private BigDecimal costoTotal = BigDecimal.ZERO;

    /** UUID generado por el cliente Flutter offline: deduplica el reintento. */
    @Column(name = "origen_offline_id", updatable = false) private UUID origenOfflineId;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("linea ASC")
    private List<VentaLinea> lineas = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoVenta> pagos = new ArrayList<>();

    public enum EstadoVenta {
        BORRADOR, PENDIENTE_STOCK, CONFIRMADA, DEVUELTA_PARCIAL, DEVUELTA, ANULADA
    }

    // ---- Comportamiento del agregado ----

    public void agregarLinea(VentaLinea linea) {
        exigirBorrador();
        linea.asignarA(this, (short) (lineas.size() + 1));
        lineas.add(linea);
        recalcular();
    }

    /** Los totales se persisten, no se derivan en la consulta: un reporte no
     *  debe recalcular impuestos y una factura emitida no puede cambiar. */
    private void recalcular() {
        subtotal       = sumar(VentaLinea::getSubtotal);
        descuentoTotal = sumar(VentaLinea::getDescuentoValor);
        impuestoTotal  = sumar(VentaLinea::getImpuestoValor);
        costoTotal     = sumar(VentaLinea::getCostoTotal);
        total = subtotal.subtract(descuentoTotal).add(impuestoTotal)
                        .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal sumar(java.util.function.Function<VentaLinea, BigDecimal> f) {
        return lineas.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Paso 1 de la saga. NO descuenta stock: publica la solicitud y queda
     * en PENDIENTE_STOCK hasta que Inventario responda.
     */
    public OutboxEvento solicitarReservaDeStock(UUID correlacionId) {
        exigirBorrador();
        if (lineas.isEmpty()) throw new IllegalStateException("Una venta sin líneas no se confirma");
        this.estado = EstadoVenta.PENDIENTE_STOCK;
        return OutboxEvento.de(getNegocioId(), "Venta", getId(), "solicitar_reserva_stock",
            Map.of("venta_id", getId(), "bodega_id", bodegaId, "correlacion_id", correlacionId,
                   "lineas", lineas.stream().map(VentaLinea::aSolicitudStock).toList()));
    }

    /** Paso 3a: Inventario confirmó. Recién ahora la venta es un hecho. */
    public OutboxEvento confirmar() {
        if (estado != EstadoVenta.PENDIENTE_STOCK)
            throw new IllegalStateException("Solo se confirma una venta que esperaba stock");
        this.estado = EstadoVenta.CONFIRMADA;
        return OutboxEvento.de(getNegocioId(), "Venta", getId(), "venta_completada",
            Map.of("venta_id", getId(), "numero", numero, "cliente_id", clienteId,
                   "total", total, "fecha", fecha.toString()));
    }

    /** Paso 3b: compensación. No hay @Transactional que cubra dos servicios. */
    public void revertirPorFaltaDeStock(String motivo) {
        this.estado = EstadoVenta.BORRADOR;
    }

    private void exigirBorrador() {
        if (estado != EstadoVenta.BORRADOR)
            throw new IllegalStateException("La venta ya no es modificable: " + estado);
    }

    public List<VentaLinea> getLineas() { return List.copyOf(lineas); }
    public BigDecimal getTotal() { return total; }
    public EstadoVenta getEstado() { return estado; }
}
