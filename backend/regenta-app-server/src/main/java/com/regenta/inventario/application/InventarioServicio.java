package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.LotePorVencer;

@Service
@Transactional(readOnly = true)
public class InventarioServicio implements InventarioApi {

    private final GestionDeLotes lotes;
    private final GestionDeProductos productos;
    private final GestionDeReservasStock reservas;
    private final GestionDeFracciones fracciones;
    private final com.regenta.inventario.infrastructure.BodegaRepositorio bodegas;
    private final SalidaPorAnulacionDeCompra salidaPorAnulacion;
    private final PreciosParaCompras precios;
    /** HU-132 · Nulo solo en pruebas que no lo usan. */
    private final PreciosPorLista preciosPorLista;

    public InventarioServicio(GestionDeLotes lotes,
                             GestionDeProductos productos,
                             GestionDeReservasStock reservas,
                             GestionDeFracciones fracciones,
                             com.regenta.inventario.infrastructure.BodegaRepositorio bodegas,
                             SalidaPorAnulacionDeCompra salidaPorAnulacion,
                             PreciosParaCompras precios) {
        this(lotes, productos, reservas, fracciones, bodegas, salidaPorAnulacion, precios, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public InventarioServicio(GestionDeLotes lotes,
                             GestionDeProductos productos,
                             GestionDeReservasStock reservas,
                             GestionDeFracciones fracciones,
                             com.regenta.inventario.infrastructure.BodegaRepositorio bodegas,
                             SalidaPorAnulacionDeCompra salidaPorAnulacion,
                             PreciosParaCompras precios,
                             PreciosPorLista preciosPorLista) {
        this.preciosPorLista = preciosPorLista;
        this.precios = precios;
        this.bodegas = bodegas;
        this.salidaPorAnulacion = salidaPorAnulacion;
        this.lotes = lotes;
        this.productos = productos;
        this.reservas = reservas;
        this.fracciones = fracciones;
    }

    @Override
    public List<com.regenta.inventario.api.FraccionDeCatalogo> fraccionesDe(UUID negocioId,
            UUID productoId) {
        return fracciones.fraccionesSinPermiso(negocioId, productoId).stream()
                .map(f -> new com.regenta.inventario.api.FraccionDeCatalogo(f.medida(), f.factor(),
                        f.precio()))
                .toList();
    }

    @Override
    public java.util.Optional<com.regenta.inventario.api.ListaDePreciosInfo> listaDePrecios(UUID negocioId,
            UUID listaId) {
        return preciosPorLista.lista(negocioId, listaId);
    }

    @Override
    public java.util.Optional<BigDecimal> precioEnLista(UUID negocioId, UUID listaId, UUID productoId,
            BigDecimal cantidad) {
        return preciosPorLista.precio(negocioId, listaId, productoId, cantidad);
    }

    @Override
    public List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias) {
        return lotes.porVencer(ventanaDias);
    }

    @Override
    public Map<UUID, BigDecimal> costoUnitarioDe(UUID negocioId, Collection<UUID> productoIds) {
        return productos.costos(productoIds);
    }

    @Override
    public java.util.Optional<com.regenta.inventario.api.PrecioDeCatalogo> precioDeCatalogo(
            UUID negocioId, UUID productoId) {
        return productos.precioDeCatalogo(negocioId, productoId);
    }

    @Override
    @Transactional
    public void confirmarReserva(String origenTipo, UUID origenId) {
        reservas.confirmar(origenTipo, origenId);
    }

    @Override
    public Map<UUID, com.regenta.inventario.api.ReglaDeLote> reglasDeLote(UUID negocioId,
            Collection<UUID> productoIds) {
        return productos.reglasDeLote(negocioId, productoIds);
    }

    @Override
    public Map<UUID, com.regenta.inventario.api.ProductoResumido> resumenDe(UUID negocioId,
            Collection<UUID> productoIds) {
        return productos.resumenDe(negocioId, productoIds);
    }

    @Override
    public Map<UUID, com.regenta.inventario.api.ProductoParaComprar> paraComprar(UUID negocioId,
            Collection<UUID> productoIds) {
        return productos.paraComprar(negocioId, productoIds);
    }

    @Override
    public boolean bodegaActiva(UUID negocioId, UUID bodegaId) {
        return bodegaId != null && bodegas.findById(bodegaId)
                .filter(b -> negocioId.equals(b.getNegocioId()) && b.isActiva())
                .isPresent();
    }

    @Override
    public List<String> faltantesParaSacar(UUID negocioId, UUID bodegaId,
            Collection<com.regenta.inventario.api.SalidaPrevista> salidas) {
        return salidaPorAnulacion.faltantes(negocioId, bodegaId, salidas);
    }

    @Override
    public Map<UUID, com.regenta.inventario.api.CostoYPrecio> costosYPrecios(UUID negocioId, UUID bodegaId,
            Collection<UUID> productoIds) {
        return precios.costosYPrecios(negocioId, bodegaId, productoIds);
    }

    @Override
    @Transactional
    public void cambiarPrecioDeVenta(UUID negocioId, UUID productoId, BigDecimal precioConIva) {
        precios.cambiarPrecioDeVenta(negocioId, productoId, precioConIva);
    }

    /**
     * HU-180 · Pasa por {@link GestionDeProductos#crear}, el mismo camino de la ficha: su permiso, sus
     * duplicados de SKU y código de barras y su tarifa obligatoria.
     */
    @Override
    @Transactional
    public UUID crearProducto(UUID negocioId, com.regenta.inventario.api.ProductoNuevo nuevo) {
        com.regenta.inventario.domain.TarifaIva tarifa = null;
        if (nuevo.tarifaIva() != null && !nuevo.tarifaIva().isBlank()) {
            try {
                tarifa = com.regenta.inventario.domain.TarifaIva.valueOf(nuevo.tarifaIva().trim());
            } catch (IllegalArgumentException noExiste) {
                throw new com.regenta.shared.web.ReglaDeNegocioException(
                        "La tarifa de IVA es IVA_19, IVA_5, EXENTO o EXCLUIDO, no " + nuevo.tarifaIva());
            }
        }
        return productos.crear(new SolicitudDeProducto(nuevo.sku(), nuevo.codigoBarras(), nuevo.nombre(), null,
                nuevo.categoriaId(), nuevo.unidadMedidaId(), null, null, nuevo.precioVenta(), tarifa, null, null,
                null, nuevo.manejaLotes(), false, nuevo.perecedero(), false, null)).id();
    }
}
