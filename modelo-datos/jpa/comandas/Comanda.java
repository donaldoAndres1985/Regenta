package com.regenta.comandas.dominio;

import com.regenta.comun.dominio.EntidadTenant;
import com.regenta.comun.mensajeria.OutboxEvento;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Agregado raíz del patrón Comanda.
 *
 * Lo que lo hace distinto de Venta: la transacción queda ABIERTA y acumula
 * líneas durante 90 minutos. Consecuencias en el modelo de dominio:
 *
 *  1. Cada LÍNEA tiene su propia máquina de estados (pendiente → enviada →
 *     en preparación → lista → entregada), no solo la cabecera.
 *  2. Los totales se recalculan en cada adición, no una sola vez al cerrar.
 *  3. Anular una línea YA ENVIADA a cocina genera merma (el insumo se gastó);
 *     anularla antes de enviarla no. Son dos operaciones distintas.
 *  4. La cuenta se puede dividir entre comensales sin rehacer la comanda.
 */
@Entity
@Table(name = "comandas", schema = "comandas")
public class Comanda extends EntidadTenant {

    @Column(name = "numero", nullable = false, length = 30) private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoComanda tipo = TipoComanda.MESA;

    @Column(name = "mesa_id")          private UUID mesaId;         // [ref lógica]
    @Column(name = "sesion_mesa_id")   private UUID sesionMesaId;   // [ref lógica]
    @Column(name = "cliente_id")       private UUID clienteId;
    @Column(name = "mesero_usuario_id") private UUID meseroUsuarioId;
    @Column(name = "num_comensales", nullable = false) private short numComensales = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoComanda estado = EstadoComanda.ABIERTA;

    @Column(name = "subtotal",       nullable = false, precision = 16, scale = 4) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "impuesto_total", nullable = false, precision = 16, scale = 4) private BigDecimal impuestoTotal = BigDecimal.ZERO;
    @Column(name = "propina",        nullable = false, precision = 16, scale = 4) private BigDecimal propina = BigDecimal.ZERO;
    @Column(name = "total",          nullable = false, precision = 16, scale = 4) private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "abierta_en", nullable = false) private OffsetDateTime abiertaEn = OffsetDateTime.now();
    @Column(name = "enviada_cocina_en") private OffsetDateTime enviadaCocinaEn;
    @Column(name = "cerrada_en") private OffsetDateTime cerradaEn;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("linea ASC")
    private List<ComandaLinea> lineas = new ArrayList<>();

    public enum TipoComanda { MESA, PARA_LLEVAR, DOMICILIO, BARRA, HABITACION }
    public enum EstadoComanda { ABIERTA, EN_COCINA, SERVIDA, CUENTA_PEDIDA, CERRADA, ANULADA }

    /** A diferencia de una venta, se pueden seguir agregando ítems después
     *  de haber enviado los primeros a cocina. */
    public void agregarLinea(ComandaLinea linea) {
        if (estado == EstadoComanda.CERRADA || estado == EstadoComanda.ANULADA)
            throw new IllegalStateException("La comanda está cerrada");
        linea.asignarA(this, (short) (lineas.size() + 1));
        lineas.add(linea);
        recalcular();
    }

    /** Envía a cocina solo las líneas pendientes, agrupadas por estación. */
    public Map<UUID, List<ComandaLinea>> enviarACocina() {
        List<ComandaLinea> pendientes = lineas.stream()
            .filter(l -> l.getEstado() == ComandaLinea.EstadoLinea.PENDIENTE).toList();
        if (pendientes.isEmpty()) return Map.of();
        pendientes.forEach(ComandaLinea::marcarEnviada);
        if (enviadaCocinaEn == null) enviadaCocinaEn = OffsetDateTime.now();
        estado = EstadoComanda.EN_COCINA;
        return new HashMap<>(pendientes.stream()
            .collect(java.util.stream.Collectors.groupingBy(ComandaLinea::getEstacionId)));
    }

    /**
     * Cierre. Emite DOS eventos: el equivalente a `venta_completada` para
     * Facturación/Reportes/Caja, y `insumos_consumidos` — la explosión de
     * recetas que Inventario necesita para descontar carne, pan y cebolla.
     * Sin ese segundo evento el patrón Comanda queda desconectado del stock.
     */
    public List<OutboxEvento> cerrar(BigDecimal propinaRecibida) {
        if (estado == EstadoComanda.CERRADA || estado == EstadoComanda.ANULADA)
            throw new IllegalStateException("La comanda ya está cerrada");
        if (lineas.stream().anyMatch(l -> l.getEstado() == ComandaLinea.EstadoLinea.PENDIENTE))
            throw new IllegalStateException("Hay ítems sin enviar a cocina");

        this.propina = propinaRecibida;
        this.estado = EstadoComanda.CERRADA;
        this.cerradaEn = OffsetDateTime.now();
        recalcular();

        return List.of(
            OutboxEvento.de(getNegocioId(), "Comanda", getId(), "pedido_completado",
                Map.of("comanda_id", getId(), "numero", numero, "total", total,
                       "propina", propina, "mesa_id", mesaId)),
            OutboxEvento.de(getNegocioId(), "Comanda", getId(), "insumos_consumidos",
                Map.of("comanda_id", getId(),
                       "items", lineas.stream()
                           .filter(l -> l.getEstado() != ComandaLinea.EstadoLinea.ANULADA)
                           .map(ComandaLinea::aConsumoDeInsumos).toList()))
        );
    }

    private void recalcular() {
        subtotal = lineas.stream().filter(l -> l.getEstado() != ComandaLinea.EstadoLinea.ANULADA)
                         .map(ComandaLinea::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        impuestoTotal = lineas.stream().filter(l -> l.getEstado() != ComandaLinea.EstadoLinea.ANULADA)
                              .map(ComandaLinea::getImpuestoValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.add(impuestoTotal).add(propina);
    }

    public EstadoComanda getEstado() { return estado; }
    public BigDecimal getTotal() { return total; }
}
