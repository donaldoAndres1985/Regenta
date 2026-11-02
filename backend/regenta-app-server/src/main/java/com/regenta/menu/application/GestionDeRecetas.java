package com.regenta.menu.application;

import com.regenta.menu.api.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.menu.application.RecetaDelItem.LineaDeReceta;
import com.regenta.menu.domain.ItemDeMenu;
import com.regenta.menu.domain.Receta;
import com.regenta.menu.infrastructure.ItemDeMenuRepositorio;
import com.regenta.menu.infrastructure.RecetaRepositorio;

/**
 * La receta de un Ã­tem (HU-079): quÃ© insumos de inventario consume, con cantidad
 * y merma (criterio 1). Cada cambio recalcula el {@code costo_estimado} del Ã­tem
 * a partir de los costos de servicio-inventario (criterio 2). Un Ã­tem sin receta
 * simplemente no tiene lÃ­neas (criterio 5).
 */
@Service
public class GestionDeRecetas {

    private final RecetaRepositorio recetas;
    private final ItemDeMenuRepositorio items;
    private final CostosDeInventario costos;

    public GestionDeRecetas(RecetaRepositorio recetas, ItemDeMenuRepositorio items,
            CostosDeInventario costos) {
        this.recetas = recetas;
        this.items = items;
        this.costos = costos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public RecetaDelItem ver(UUID itemMenuId) {
        ItemDeMenu item = itemDelNegocio(itemMenuId);
        return new RecetaDelItem(itemMenuId, item.getCostoEstimado(),
                recetas.findByItemMenuIdOrderByNombreSnapshotAscProductoIdAsc(itemMenuId).stream()
                        .map(LineaDeReceta::de).toList());
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public RecetaDelItem agregarLinea(UUID itemMenuId, SolicitudDeLineaDeReceta solicitud) {
        ItemDeMenu item = itemDelNegocio(itemMenuId);
        if (recetas.existsByItemMenuIdAndProductoId(itemMenuId, solicitud.productoId())) {
            throw new RecursoDuplicadoException("Ese insumo ya estÃ¡ en la receta");
        }
        Receta linea = Receta.crear(item.getNegocioId(), itemMenuId, solicitud.productoId(),
                solicitud.nombreSnapshot(), solicitud.cantidad(), solicitud.unidad(),
                solicitud.mermaPct(), solicitud.opcional());
        try {
            recetas.save(linea);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ese insumo ya estÃ¡ en la receta");
        }
        recalcular(item);
        return ver(itemMenuId);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public RecetaDelItem actualizarLinea(UUID lineaId, SolicitudDeLineaDeReceta solicitud) {
        Receta linea = lineaDelNegocio(lineaId);
        linea.editar(solicitud.nombreSnapshot(), solicitud.cantidad(), solicitud.unidad(),
                solicitud.mermaPct(), solicitud.opcional());
        recetas.save(linea);
        recalcular(itemDelNegocio(linea.getItemMenuId()));
        return ver(linea.getItemMenuId());
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public RecetaDelItem quitarLinea(UUID lineaId) {
        Receta linea = lineaDelNegocio(lineaId);
        UUID itemMenuId = linea.getItemMenuId();
        recetas.delete(linea);
        recetas.flush();
        recalcular(itemDelNegocio(itemMenuId));
        return ver(itemMenuId);
    }

    /** Reemplaza toda la receta del Ã­tem por la lista recibida (HU-079). */
    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public RecetaDelItem reemplazar(UUID itemMenuId, List<SolicitudDeLineaDeReceta> lineas) {
        ItemDeMenu item = itemDelNegocio(itemMenuId);
        recetas.deleteAll(
                recetas.findByItemMenuIdOrderByNombreSnapshotAscProductoIdAsc(itemMenuId));
        recetas.flush();
        if (lineas != null) {
            for (SolicitudDeLineaDeReceta s : lineas) {
                recetas.save(Receta.crear(item.getNegocioId(), itemMenuId, s.productoId(),
                        s.nombreSnapshot(), s.cantidad(), s.unidad(), s.mermaPct(), s.opcional()));
            }
        }
        recalcular(item);
        return ver(itemMenuId);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public RecetaDelItem recalcularCosto(UUID itemMenuId) {
        recalcular(itemDelNegocio(itemMenuId));
        return ver(itemMenuId);
    }

    /**
     * Recalcula el costo estimado de todos los Ã­tems cuya receta usa ese
     * producto (HU-079 criterio 2). Lo llama el consumidor de cambios de costo.
     */
    @Transactional
    public void recalcularItemsQueUsan(UUID negocioId, UUID productoId) {
        for (UUID itemMenuId : recetas.itemsQueUsan(negocioId, productoId)) {
            items.findById(itemMenuId).ifPresent(this::recalcular);
        }
    }

    private void recalcular(ItemDeMenu item) {
        List<Receta> lineas = recetas
                .findByItemMenuIdOrderByNombreSnapshotAscProductoIdAsc(item.getId());
        if (lineas.isEmpty()) {
            item.fijarCostoEstimado(BigDecimal.ZERO);
            items.save(item);
            return;
        }
        Map<UUID, BigDecimal> costoPorProducto = costos.costoUnitarioDe(item.getNegocioId(),
                lineas.stream().map(Receta::getProductoId).distinct().toList());
        BigDecimal total = BigDecimal.ZERO;
        for (Receta l : lineas) {
            total = total.add(l.costoCon(costoPorProducto.get(l.getProductoId())));
        }
        item.fijarCostoEstimado(total);
        items.save(item);
    }

    private ItemDeMenu itemDelNegocio(UUID itemMenuId) {
        return items.findByIdAndNegocioId(itemMenuId, ContextoDeNegocio.negocioActual())
                .filter(i -> !i.estaEliminado())
                .orElseThrow(() -> new NoEncontradoException("Ese Ã­tem no existe"));
    }

    private Receta lineaDelNegocio(UUID lineaId) {
        return recetas.findByIdAndNegocioId(lineaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa lÃ­nea de receta no existe"));
    }
}
