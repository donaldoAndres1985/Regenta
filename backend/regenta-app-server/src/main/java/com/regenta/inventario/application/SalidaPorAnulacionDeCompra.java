package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.api.SalidaPrevista;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.ExistenciaLoteRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.LoteRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;

/**
 * HU-177 · Anular una recepción saca de la bodega lo que entró con ella, del mismo lote (C4), y
 * deshace su efecto en el costo promedio. Antes de anular, Compras pregunta si alcanza (C3): si ya
 * se vendió parte, no se anula.
 */
@Service
public class SalidaPorAnulacionDeCompra {

    private static final int ESCALA_COSTO = 4;

    private final LibroMayorDeInventario libro;
    private final ExistenciaRepositorio existencias;
    private final ExistenciaLoteRepositorio existenciasLote;
    private final LoteRepositorio lotes;
    private final ProductoRepositorio productos;

    public SalidaPorAnulacionDeCompra(LibroMayorDeInventario libro, ExistenciaRepositorio existencias,
            ExistenciaLoteRepositorio existenciasLote, LoteRepositorio lotes, ProductoRepositorio productos) {
        this.libro = libro;
        this.existencias = existencias;
        this.existenciasLote = existenciasLote;
        this.lotes = lotes;
        this.productos = productos;
    }

    /**
     * C3 · Lo que no alcanza para sacar, una frase por producto o lote; vacía si todo alcanza. Cuenta
     * lo disponible (sin lo reservado para ventas en curso) y suma las líneas repetidas.
     */
    @Transactional(readOnly = true)
    public List<String> faltantes(UUID negocioId, UUID bodegaId, Collection<SalidaPrevista> salidas) {
        Map<UUID, BigDecimal> porProducto = new LinkedHashMap<>();
        Map<String, BigDecimal> porLote = new LinkedHashMap<>();
        Map<String, UUID> productoDelLote = new LinkedHashMap<>();
        for (SalidaPrevista s : salidas) {
            porProducto.merge(s.productoId(), s.cantidad(), BigDecimal::add);
            String codigo = s.codigoLote() == null ? "" : s.codigoLote().trim();
            if (!codigo.isEmpty()) {
                porLote.merge(s.productoId() + "|" + codigo, s.cantidad(), BigDecimal::add);
                productoDelLote.put(s.productoId() + "|" + codigo, s.productoId());
            }
        }

        List<String> problemas = new ArrayList<>();
        porProducto.forEach((producto, cantidad) -> {
            BigDecimal hay = existencias.findByProductoIdAndBodegaId(producto, bodegaId)
                    .map(Existencia::disponible).orElse(BigDecimal.ZERO);
            if (hay.compareTo(cantidad) < 0) {
                problemas.add("De «" + nombre(producto) + "» quedan " + plano(hay) + " y se tendrían que sacar "
                        + plano(cantidad));
            }
        });
        porLote.forEach((clave, cantidad) -> {
            UUID producto = productoDelLote.get(clave);
            String codigo = clave.substring(clave.indexOf('|') + 1);
            BigDecimal hay = lotes.findByNegocioIdAndProductoIdAndCodigoLote(negocioId, producto, codigo)
                    .flatMap(l -> existenciasLote.findByLoteIdAndBodegaId(l.getId(), bodegaId))
                    .map(e -> e.getCantidad()).orElse(BigDecimal.ZERO);
            if (hay.compareTo(cantidad) < 0) {
                problemas.add("Del lote " + codigo + " de «" + nombre(producto) + "» quedan " + plano(hay)
                        + " y se tendrían que sacar " + plano(cantidad));
            }
        });
        return problemas;
    }

    /** Saca cada línea de la recepción anulada. Idempotente: un evento repetido no saca dos veces. */
    @Transactional
    public void sacar(EntradaDeRecepcion anulada) {
        List<EntradaDeRecepcion.Linea> lineas = anulada.lineas();
        for (int i = 0; i < lineas.size(); i++) {
            EntradaDeRecepcion.Linea l = lineas.get(i);
            UUID bodega = anulada.bodegaId();
            Existencia antes = existencias.findByProductoIdAndBodegaId(l.productoId(), bodega).orElse(null);
            BigDecimal cantidadAntes = antes == null ? BigDecimal.ZERO : antes.getCantidad();
            BigDecimal promedioAntes = antes == null || antes.getCostoPromedio() == null
                    ? BigDecimal.ZERO : antes.getCostoPromedio();

            RegistroDeMovimiento m = libro.registrar(new SolicitudDeMovimiento(l.productoId(), bodega,
                    TipoMovimiento.SALIDA_ANULACION_COMPRA, l.cantidad(), OrigenMovimiento.COMPRA,
                    anulada.recepcionId(), "Anulación de la recepción " + anulada.numero(),
                    "anulacion-recepcion:" + anulada.recepcionId() + ":" + i, l.codigoLote(), null, null));
            if (m.duplicado()) {
                continue;
            }
            BigDecimal costo = l.costoUnitario() == null ? BigDecimal.ZERO : l.costoUnitario();
            BigDecimal promedio = promedioSinLaCompra(cantidadAntes, promedioAntes, l.cantidad(), costo);
            existencias.findByProductoIdAndBodegaId(l.productoId(), bodega).ifPresent(e -> {
                e.fijarCostoPromedio(promedio);
                existencias.save(e);
            });
        }
    }

    /**
     * El promedio como si esas unidades no hubieran entrado. Si no queda nada, o el resultado no tiene
     * sentido (las salidas intermedias lo distorsionaron), se deja el promedio que había.
     */
    static BigDecimal promedioSinLaCompra(BigDecimal cantidadAntes, BigDecimal promedioAntes,
            BigDecimal cantidad, BigDecimal costo) {
        BigDecimal quedan = cantidadAntes.subtract(cantidad);
        if (quedan.signum() <= 0) {
            return promedioAntes;
        }
        BigDecimal valor = cantidadAntes.multiply(promedioAntes).subtract(cantidad.multiply(costo));
        if (valor.signum() < 0) {
            return promedioAntes;
        }
        return valor.divide(quedan, ESCALA_COSTO, RoundingMode.HALF_UP);
    }

    private String nombre(UUID productoId) {
        return productos.findById(productoId).map(p -> p.getNombre()).orElse("producto");
    }

    private static String plano(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
