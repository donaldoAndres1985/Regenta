package com.regenta.inventario.application;

import java.util.List;
import java.util.UUID;

import com.regenta.inventario.domain.TablaDeFracciones;

/** HU-156 · una tabla de fracciones y cuántos productos la usan. */
public record TablaDeFraccionesDelNegocio(UUID id, String nombre, List<FraccionDelNegocio> fracciones,
        long productos) {

    static TablaDeFraccionesDelNegocio de(TablaDeFracciones t, long productos) {
        return new TablaDeFraccionesDelNegocio(t.getId(), t.getNombre(), t.getFracciones().stream()
                .map(f -> new FraccionDelNegocio(f.getMedida(), f.getFactor(), f.getPrecio(), "TABLA"))
                .toList(), productos);
    }
}
