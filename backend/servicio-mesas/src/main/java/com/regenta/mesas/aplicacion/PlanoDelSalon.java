package com.regenta.mesas.aplicacion;

import java.util.List;

/**
 * El plano del salón: las zonas en orden y las mesas de cada una con su posición
 * (HU-081). Las mesas sin zona van en {@code sinZona}.
 */
public record PlanoDelSalon(List<ZonaConMesas> zonas, List<MesaDelNegocio> sinZona) {

    public record ZonaConMesas(ZonaDelNegocio zona, List<MesaDelNegocio> mesas) {
    }
}
