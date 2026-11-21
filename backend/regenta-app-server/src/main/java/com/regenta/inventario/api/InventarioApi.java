package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato público que el módulo inventario expone a otros módulos (alertas, menú, ventas, compras).
 */
public interface InventarioApi {

    /** Lotes próximos a vencer dentro de la ventana de días configurada. */
    List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias);

    /** Costo unitario por producto de los insumos solicitados. */
    Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds);

    /**
     * HU-148 · precio con IVA incluido y tarifa de un producto activo del negocio; vacío si no
     * existe, es de otro negocio o está inactivo.
     */
    java.util.Optional<PrecioDeCatalogo> precioDeCatalogo(UUID negocioId, UUID productoId);

    /** HU-158 · las fracciones con que se vende un producto del negocio (vacío si no tiene). */
    List<FraccionDeCatalogo> fraccionesDe(UUID negocioId, UUID productoId);

    /** Confirma una reserva de stock previa, convirtiéndola en salida definitiva. */
    void confirmarReserva(String origenTipo, UUID origenId);

    /**
     * HU-165 · Qué exige cada producto al recibirlo (lote, vencimiento). Los productos que no son
     * del negocio no vienen en el mapa.
     */
    Map<UUID, ReglaDeLote> reglasDeLote(UUID negocioId, Collection<UUID> productoIds);

    /** HU-166 · SKU y nombre de cada producto del negocio; los de otro negocio no vienen. */
    Map<UUID, ProductoResumido> resumenDe(UUID negocioId, Collection<UUID> productoIds);

    /** HU-167 · Tarifa de IVA y regla de lote de cada producto del negocio, para comprarlo. */
    Map<UUID, ProductoParaComprar> paraComprar(UUID negocioId, Collection<UUID> productoIds);

    /** HU-167 · Si la bodega existe, es del negocio y está activa. */
    boolean bodegaActiva(UUID negocioId, UUID bodegaId);

    /**
     * HU-177 C3 · Si alcanza para sacar esto de la bodega (lo disponible, y del lote cuando se indica).
     * Devuelve qué falta, una frase por producto o lote; vacía si alcanza todo.
     */
    List<String> faltantesParaSacar(UUID negocioId, UUID bodegaId, Collection<SalidaPrevista> salidas);

    /** HU-179 C1 · El costo promedio en la bodega y el precio con su base, de los productos del negocio. */
    Map<UUID, CostoYPrecio> costosYPrecios(UUID negocioId, UUID bodegaId, Collection<UUID> productoIds);

    /**
     * HU-179 C2 · Cambia el precio de venta (IVA incluido). Quien llama ya exigió
     * {@code INVENTARIO_PRODUCTO_EDITAR}.
     */
    void cambiarPrecioDeVenta(UUID negocioId, UUID productoId, BigDecimal precioConIva);
}
