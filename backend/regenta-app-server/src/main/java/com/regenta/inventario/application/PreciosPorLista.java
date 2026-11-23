package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.api.ListaDePreciosInfo;
import com.regenta.inventario.domain.ListaDePrecios;
import com.regenta.inventario.domain.PrecioDeProducto;
import com.regenta.inventario.infrastructure.ListaDePreciosRepositorio;
import com.regenta.inventario.infrastructure.PrecioDeProductoRepositorio;

/**
 * HU-132 · Las listas de precios (HU-036) vistas desde otros módulos: una lista por su id y el
 * precio de un producto en ella. Solo cuentan las listas activas y
 * vigentes hoy; si una no aplica, se responde vacío y quien pregunta sigue con el catálogo. Sin
 * permisos propios: quien llama ya exigió el suyo (vender, editar un cliente).
 */
@Component
public class PreciosPorLista {

    private final ListaDePreciosRepositorio listas;
    private final PrecioDeProductoRepositorio precios;

    public PreciosPorLista(ListaDePreciosRepositorio listas, PrecioDeProductoRepositorio precios) {
        this.listas = listas;
        this.precios = precios;
    }

    @Transactional(readOnly = true)
    public Optional<ListaDePreciosInfo> lista(UUID negocioId, UUID listaId) {
        if (listaId == null) {
            return Optional.empty();
        }
        return listas.findById(listaId)
                .filter(l -> l.getNegocioId().equals(negocioId) && l.vigente(LocalDate.now()))
                .map(PreciosPorLista::info);
    }

    /** El tramo que aplica a la cantidad (HU-036 C2): el de mayor cantidad mínima que no la pase. */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> precio(UUID negocioId, UUID listaId, UUID productoId, BigDecimal cantidad) {
        if (lista(negocioId, listaId).isEmpty()) {
            return Optional.empty();
        }
        List<PrecioDeProducto> tramos = precios.findByListaIdAndProductoIdOrderByCantidadMinimaAsc(listaId, productoId);
        if (tramos.isEmpty()) {
            return Optional.empty();
        }
        PrecioDeProducto aplica = tramos.get(0);
        BigDecimal c = cantidad == null ? BigDecimal.ONE : cantidad;
        for (PrecioDeProducto tramo : tramos) {
            if (tramo.getCantidadMinima().compareTo(c) <= 0) {
                aplica = tramo;
            }
        }
        return Optional.of(aplica.getPrecio());
    }

    private static ListaDePreciosInfo info(ListaDePrecios l) {
        return new ListaDePreciosInfo(l.getId(), l.getNombre());
    }
}
