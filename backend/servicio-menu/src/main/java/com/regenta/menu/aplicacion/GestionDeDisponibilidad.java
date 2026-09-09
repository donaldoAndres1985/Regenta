package com.regenta.menu.aplicacion;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.menu.domain.DisponibilidadDiaria;
import com.regenta.menu.domain.ItemDeMenu;
import com.regenta.menu.infra.DisponibilidadDiariaRepositorio;
import com.regenta.menu.infra.ItemDeMenuRepositorio;
import com.regenta.menu.infra.RecetaRepositorio;

/**
 * Disponibilidad diaria de ítems (HU-080). El jefe de cocina marca un plato
 * agotado durante el servicio y desaparece de la carta del mesero (criterio 1);
 * cada fecha es una fila aparte, así que al cambiar de día se reinicia
 * (criterio 2); un {@code stock_actualizado} que deja un insumo en cero agota los
 * ítems que lo usan (criterio 3); y un ítem con cupo diario se agota solo al
 * llegar a la cuenta (criterio 4).
 *
 * <p>Publica {@code item_agotado} y {@code item_disponible} para que la carta del
 * mesero (y la KDS) reaccionen en tiempo real.
 */
@Service
public class GestionDeDisponibilidad {

    private final DisponibilidadDiariaRepositorio disponibilidad;
    private final ItemDeMenuRepositorio items;
    private final RecetaRepositorio recetas;
    private final RegistroDeEventos eventos;

    public GestionDeDisponibilidad(DisponibilidadDiariaRepositorio disponibilidad,
            ItemDeMenuRepositorio items, RecetaRepositorio recetas, RegistroDeEventos eventos) {
        this.disponibilidad = disponibilidad;
        this.items = items;
        this.recetas = recetas;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MENU_PLATO_VER")
    public DisponibilidadDelItem delDia(UUID itemId, LocalDate fecha) {
        itemDelNegocio(itemId);
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        return disponibilidad.findByItemIdAndFecha(itemId, dia)
                .map(DisponibilidadDelItem::de)
                .orElse(DisponibilidadDelItem.disponible(itemId, dia));
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public DisponibilidadDelItem marcarAgotado(UUID itemId) {
        return marcarAgotado(itemId, LocalDate.now());
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public DisponibilidadDelItem marcarAgotado(UUID itemId, LocalDate fecha) {
        ItemDeMenu item = itemDelNegocio(itemId);
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        DisponibilidadDiaria fila = filaDe(item.getNegocioId(), itemId, dia);
        if (!fila.estaAgotado()) {
            fila.marcarAgotado();
            disponibilidad.save(fila);
            publicar(item.getNegocioId(), itemId, dia, "item_agotado", "MANUAL");
        }
        return DisponibilidadDelItem.de(fila);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public DisponibilidadDelItem reponer(UUID itemId) {
        ItemDeMenu item = itemDelNegocio(itemId);
        LocalDate dia = LocalDate.now();
        DisponibilidadDiaria fila = filaDe(item.getNegocioId(), itemId, dia);
        if (fila.estaAgotado()) {
            fila.reponer();
            disponibilidad.save(fila);
            publicar(item.getNegocioId(), itemId, dia, "item_disponible", "MANUAL");
        }
        return DisponibilidadDelItem.de(fila);
    }

    @Transactional
    @RequierePermiso("MENU_PLATO_EDITAR")
    public DisponibilidadDelItem fijarCupo(UUID itemId, Integer cupoDiario) {
        ItemDeMenu item = itemDelNegocio(itemId);
        LocalDate dia = LocalDate.now();
        DisponibilidadDiaria fila = filaDe(item.getNegocioId(), itemId, dia);
        boolean seAgoto = fila.fijarCupo(cupoDiario);
        disponibilidad.save(fila);
        if (seAgoto) {
            publicar(item.getNegocioId(), itemId, dia, "item_agotado", "CUPO");
        }
        return DisponibilidadDelItem.de(fila);
    }

    /**
     * Suma las ventas de una comanda cerrada y agota los ítems que llegaron a su
     * cupo (criterio 4). Interno: lo llama {@code ConsumidorDePedidos} dentro de la
     * transacción del Inbox, sin permiso de usuario.
     */
    @Transactional
    public void registrarVentas(UUID negocioId, Map<UUID, Integer> cantidadesPorItem,
            LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        for (Map.Entry<UUID, Integer> venta : cantidadesPorItem.entrySet()) {
            Integer cantidad = venta.getValue();
            if (cantidad == null || cantidad <= 0) {
                continue;
            }
            if (items.findByIdAndNegocioId(venta.getKey(), negocioId).isEmpty()) {
                continue;
            }
            DisponibilidadDiaria fila = filaDe(negocioId, venta.getKey(), dia);
            boolean seAgoto = fila.registrarVenta(cantidad);
            disponibilidad.save(fila);
            if (seAgoto) {
                publicar(negocioId, venta.getKey(), dia, "item_agotado", "CUPO");
            }
        }
    }

    /**
     * Agota los ítems cuya receta usa un insumo que quedó en cero (criterio 3).
     * Interno: lo llama {@code ConsumidorDeStock} dentro de la transacción del
     * Inbox, sin permiso de usuario.
     */
    @Transactional
    public void agotarPorInsumoSinStock(UUID negocioId, UUID productoId, LocalDate fecha) {
        LocalDate dia = fecha == null ? LocalDate.now() : fecha;
        for (UUID itemId : recetas.itemsQueUsan(negocioId, productoId)) {
            DisponibilidadDiaria fila = filaDe(negocioId, itemId, dia);
            if (!fila.estaAgotado()) {
                fila.marcarAgotado();
                disponibilidad.save(fila);
                publicar(negocioId, itemId, dia, "item_agotado", "SIN_INSUMO");
            }
        }
    }

    private DisponibilidadDiaria filaDe(UUID negocioId, UUID itemId, LocalDate dia) {
        return disponibilidad.findByItemIdAndFecha(itemId, dia)
                .orElseGet(() -> DisponibilidadDiaria.delDia(negocioId, itemId, dia));
    }

    private void publicar(UUID negocioId, UUID itemId, LocalDate dia, String tipoEvento,
            String motivo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("item_menu_id", itemId.toString());
        payload.put("fecha", dia.toString());
        payload.put("motivo", motivo);
        eventos.registrar(negocioId, "ItemDeMenu", itemId, tipoEvento, payload);
    }

    private ItemDeMenu itemDelNegocio(UUID itemId) {
        return items.findByIdAndNegocioId(itemId, ContextoDeNegocio.negocioActual())
                .filter(i -> !i.estaEliminado())
                .orElseThrow(() -> new NoEncontradoException("Ese ítem no existe"));
    }
}
