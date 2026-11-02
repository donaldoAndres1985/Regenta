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

    public InventarioServicio(GestionDeLotes lotes,
                             GestionDeProductos productos,
                             GestionDeReservasStock reservas) {
        this.lotes = lotes;
        this.productos = productos;
        this.reservas = reservas;
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
    @Transactional
    public void confirmarReserva(String origenTipo, UUID origenId) {
        reservas.confirmar(origenTipo, origenId);
    }
}
