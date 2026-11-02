package com.regenta.comandas.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.comandas.application.CatalogoDeMenu;
import com.regenta.menu.api.CotizacionModificadoresDto;
import com.regenta.menu.api.ItemDeMenuDto;
import com.regenta.menu.api.MenuApi;

/**
 * Adaptador directo hacia el módulo menú a través de MenuApi.
 */
@Component
public class CatalogoDeMenuDirecto implements CatalogoDeMenu {

    private final MenuApi menuApi;

    public CatalogoDeMenuDirecto(MenuApi menuApi) {
        this.menuApi = menuApi;
    }

    @Override
    public ItemDeMenu item(UUID negocioId, UUID itemMenuId) {
        ItemDeMenuDto dto = menuApi.item(negocioId, itemMenuId);
        if (dto == null) {
            return null;
        }
        return new ItemDeMenu(
                dto.id(),
                dto.nombre(),
                dto.precio(),
                dto.estacionId(),
                dto.curso(),
                dto.costoEstimado(),
                dto.disponible()
        );
    }

    @Override
    public Cotizacion cotizarModificadores(UUID negocioId, UUID itemMenuId, List<UUID> modificadorIds) {
        CotizacionModificadoresDto dto = menuApi.cotizarModificadores(negocioId, itemMenuId, modificadorIds);
        if (dto == null) {
            return null;
        }
        List<ModificadorElegido> elegidos = dto.elegidos() == null ? List.of() : dto.elegidos().stream()
                .map(m -> new ModificadorElegido(m.id(), m.nombre(), m.precioExtra()))
                .toList();
        return new Cotizacion(elegidos, dto.extraPorUnidad());
    }
}
