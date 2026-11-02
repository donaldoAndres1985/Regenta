package com.regenta.menu.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.menu.api.CotizacionModificadoresDto;
import com.regenta.menu.api.ItemDeMenuDto;
import com.regenta.menu.api.MenuApi;
import com.regenta.menu.api.ModificadorElegidoDto;
import com.regenta.shared.web.NoEncontradoException;

@Service
@Transactional(readOnly = true)
public class MenuServicio implements MenuApi {

    private final GestionDeItemsDeMenu items;
    private final GestionDeModificadores modificadores;

    public MenuServicio(GestionDeItemsDeMenu items, GestionDeModificadores modificadores) {
        this.items = items;
        this.modificadores = modificadores;
    }

    @Override
    public ItemDeMenuDto item(UUID negocioId, UUID itemMenuId) {
        try {
            ItemDelNegocio i = items.ver(itemMenuId);
            if (i == null || !i.activo()) {
                return null;
            }
            return new ItemDeMenuDto(i.id(), i.nombre(), i.precio(), i.estacionId(),
                    i.curso(), i.costoEstimado(), i.disponible());
        } catch (NoEncontradoException e) {
            return null;
        }
    }

    @Override
    public CotizacionModificadoresDto cotizarModificadores(UUID negocioId, UUID itemMenuId, List<UUID> modificadorIds) {
        CotizacionDeModificadores c = modificadores.cotizarSeleccion(itemMenuId, modificadorIds);
        List<ModificadorElegidoDto> elegidos = c.elegidos().stream()
                .map(e -> new ModificadorElegidoDto(e.id(), e.nombre(), e.precioExtra()))
                .toList();
        return new CotizacionModificadoresDto(elegidos, c.extraTotal());
    }
}
