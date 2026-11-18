package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.NivelStock;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/**
 * HU-153 · el inventario general: todos los productos con su existencia y su valor, filtrados.
 *
 * <ul>
 * <li>Valor a costo = existencia × costo promedio (si no hay, el último costo conocido).</li>
 * <li>Valor a precio = existencia × precio de la etiqueta, con IVA incluido (I2).</li>
 * <li>Los totales son de todo lo filtrado, no solo de la página.</li>
 * </ul>
 * Lee productos, existencias, categorías y unidades del negocio de una vez, no una consulta por
 * producto.
 * Es la fuente de la pantalla de Inventario, así que basta INVENTARIO_PRODUCTO_VER; el costo
 * solo lo ve quien tiene INVENTARIO_EXISTENCIA_VER.
 */
@Service
public class InventarioGeneral {

    private final ProductoRepositorio productos;
    private final ExistenciaRepositorio existencias;
    private final CategoriaRepositorio categorias;
    private final UnidadMedidaRepositorio unidades;

    public InventarioGeneral(ProductoRepositorio productos, ExistenciaRepositorio existencias,
            CategoriaRepositorio categorias, UnidadMedidaRepositorio unidades) {
        this.productos = productos;
        this.existencias = existencias;
        this.categorias = categorias;
        this.unidades = unidades;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_PRODUCTO_VER")
    public InventarioDelNegocio consultar(FiltroDeInventario filtro) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        boolean veCosto = ContextoDeNegocio.actual().puede("INVENTARIO_EXISTENCIA_VER");
        Map<UUID, String> nombreDeCategoria = categorias
                .findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(negocioId).stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
        Map<UUID, String> codigoDeUnidad = unidades.findByNegocioIdOrderByCodigo(negocioId).stream()
                .collect(Collectors.toMap(UnidadMedida::getId, UnidadMedida::getCodigo));
        Map<UUID, List<Existencia>> porProducto = new HashMap<>();
        for (Existencia e : existencias.findByNegocioId(negocioId)) {
            if (filtro.bodegaId() == null || filtro.bodegaId().equals(e.getBodegaId())) {
                porProducto.computeIfAbsent(e.getProductoId(), k -> new ArrayList<>()).add(e);
            }
        }
        List<String> palabras = palabras(filtro.q());

        List<FilaDeInventario> filas = new ArrayList<>();
        int bajoMinimo = 0;
        for (Producto p : productos.findByNegocioIdOrderByNombreAsc(negocioId)) {
            if (!pasaEstado(p, filtro.estado())
                    || (filtro.categoriaId() != null && !filtro.categoriaId().equals(p.getCategoriaId()))
                    || (filtro.tarifaPorRevisar() && !p.isTarifaPorRevisar())
                    || !contieneTodas(p, palabras)) {
                continue;
            }
            List<Existencia> deEste = porProducto.getOrDefault(p.getId(), List.of());
            BigDecimal existencia = deEste.stream().map(Existencia::getCantidad)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (filtro.existenciaNegativa() && existencia.signum() >= 0) {
                continue;
            }
            NivelStock nivel = nivelDe(existencia, p.getStockMinimo());
            if (nivel == NivelStock.BAJO) {
                bajoMinimo++;
            }
            if (filtro.soloBajoMinimo() && nivel != NivelStock.BAJO) {
                continue;
            }
            BigDecimal costo = costoDe(p, deEste, existencia);
            filas.add(new FilaDeInventario(p.getId(), p.getSku(), p.getNombre(),
                    nombreDeCategoria.getOrDefault(p.getCategoriaId(), ""), p.getTarifaIva(),
                    p.isTarifaPorRevisar(), existencia, veCosto ? costo : null, p.getPrecioVenta(),
                    veCosto ? existencia.multiply(costo) : null,
                    existencia.multiply(p.getPrecioVenta()), p.isActivo(), p.getStockMinimo(), nivel,
                    codigoDeUnidad.getOrDefault(p.getUnidadMedidaId(), "")));
        }

        BigDecimal valorCosto = !veCosto ? null : filas.stream().map(FilaDeInventario::valorCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorPrecio = filas.stream().map(FilaDeInventario::valorPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int tamano = filtro.tamano() <= 0 ? 50 : Math.min(filtro.tamano(), 500);
        int desde = Math.min(Math.max(filtro.pagina(), 0) * tamano, filas.size());
        int hasta = Math.min(desde + tamano, filas.size());
        return new InventarioDelNegocio(filas.subList(desde, hasta), filas.size(), valorCosto,
                valorPrecio, bajoMinimo,
                (int) filas.stream().filter(FilaDeInventario::tarifaPorRevisar).count());
    }

    /** Agotado sin existencia; bajo mínimo si hay pero no pasa del mínimo (cuando lo tiene). */
    private static NivelStock nivelDe(BigDecimal existencia, BigDecimal minimo) {
        if (existencia.signum() <= 0) {
            return NivelStock.CERO;
        }
        if (minimo != null && minimo.signum() > 0 && existencia.compareTo(minimo) <= 0) {
            return NivelStock.BAJO;
        }
        return NivelStock.NORMAL;
    }

    /** Promedio ponderado del costo de sus bodegas; sin costo promedio, el último costo. */
    private static BigDecimal costoDe(Producto p, List<Existencia> deEste, BigDecimal existencia) {
        BigDecimal ponderado = deEste.stream()
                .map(e -> e.getCantidad().multiply(e.getCostoPromedio()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existencia.signum() > 0 && ponderado.signum() > 0) {
            return ponderado.divide(existencia, 4, java.math.RoundingMode.HALF_UP);
        }
        return p.getCostoUltimo();
    }

    private static boolean pasaEstado(Producto p, String estado) {
        if ("TODOS".equalsIgnoreCase(estado)) {
            return true;
        }
        return "INACTIVOS".equalsIgnoreCase(estado) ? !p.isActivo() : p.isActivo();
    }

    private static List<String> palabras(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return List.of(normalizar(q).trim().split("\\s+"));
    }

    private static boolean contieneTodas(Producto p, List<String> palabras) {
        String texto = normalizar(p.getNombre() + " " + p.getSku() + " "
                + (p.getCodigoBarras() == null ? "" : p.getCodigoBarras()));
        return palabras.stream().allMatch(texto::contains);
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
