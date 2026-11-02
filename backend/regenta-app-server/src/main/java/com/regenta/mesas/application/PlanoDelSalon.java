package com.regenta.mesas.application;

import com.regenta.mesas.api.*;

import java.util.List;

/**
 * El plano del salÃ³n: las zonas en orden y las mesas de cada una con su posiciÃ³n
 * (HU-081). Las mesas sin zona van en {@code sinZona}.
 */
public record PlanoDelSalon(List<ZonaConMesas> zonas, List<MesaDelNegocio> sinZona) {

    public record ZonaConMesas(ZonaDelNegocio zona, List<MesaDelNegocio> mesas) {
    }
}
