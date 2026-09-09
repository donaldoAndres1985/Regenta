package com.regenta.menu.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.menu.domain.Modificador;
import com.regenta.menu.domain.Receta;
import com.regenta.menu.infra.ModificadorRepositorio;
import com.regenta.menu.infra.RecetaRepositorio;

/**
 * Explota líneas de comanda contra las recetas de sus ítems y los insumos de sus
 * modificadores (HU-079 criterios 3, 4 y 5): devuelve, por producto, cuánto hay
 * que descontar de inventario. Vender 3 bandejas descuenta 3 veces la receta; un
 * ítem sin receta no aporta nada y no falla.
 */
@Service
public class ExplosionDeRecetas {

    private final RecetaRepositorio recetas;
    private final ModificadorRepositorio modificadores;

    public ExplosionDeRecetas(RecetaRepositorio recetas, ModificadorRepositorio modificadores) {
        this.recetas = recetas;
        this.modificadores = modificadores;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public List<InsumoAConsumir> explotar(List<LineaAExplotar> lineas) {
        return explotarPara(ContextoDeNegocio.negocioActual(), lineas);
    }

    /** Sin permiso: lo llama el consumidor de {@code pedido_completado}. */
    public List<InsumoAConsumir> explotarPara(UUID negocioId, List<LineaAExplotar> lineas) {
        Map<UUID, BigDecimal> porProducto = new LinkedHashMap<>();
        Map<UUID, String> nombre = new LinkedHashMap<>();
        if (lineas == null) {
            return List.of();
        }

        for (LineaAExplotar linea : lineas) {
            BigDecimal cantidad = linea.cantidad() == null ? BigDecimal.ONE : linea.cantidad();

            for (Receta r : recetas.findByItemMenuIdOrderByNombreSnapshotAscProductoIdAsc(
                    linea.itemMenuId())) {
                sumar(porProducto, nombre, r.getProductoId(),
                        r.cantidadConMerma().multiply(cantidad), r.getNombreSnapshot());
            }

            List<UUID> mods = linea.modificadorIds();
            if (mods != null && !mods.isEmpty()) {
                for (Modificador m : modificadores.findByIdInAndNegocioId(mods, negocioId)) {
                    if (m.getProductoId() != null && m.getCantidadInsumo() != null) {
                        sumar(porProducto, nombre, m.getProductoId(),
                                m.getCantidadInsumo().multiply(cantidad), m.getNombre());
                    }
                }
            }
        }

        List<InsumoAConsumir> salida = new ArrayList<>();
        porProducto.forEach((producto, total) -> salida.add(new InsumoAConsumir(producto,
                total.setScale(6, java.math.RoundingMode.HALF_UP), nombre.get(producto))));
        return salida;
    }

    private static void sumar(Map<UUID, BigDecimal> porProducto, Map<UUID, String> nombre,
            UUID producto, BigDecimal cantidad, String nombreProducto) {
        porProducto.merge(producto, cantidad, BigDecimal::add);
        nombre.putIfAbsent(producto, nombreProducto);
    }
}
