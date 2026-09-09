package com.regenta.menu.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.regenta.menu.domain.Carta;

/** Una carta como la ve el administrador (HU-076). */
public record CartaDelNegocio(
        UUID id,
        UUID sucursalId,
        String nombre,
        String descripcion,
        LocalTime horaDesde,
        LocalTime horaHasta,
        List<Integer> diasSemana,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean esDefault,
        boolean activa) {

    static CartaDelNegocio de(Carta c) {
        return new CartaDelNegocio(c.getId(), c.getSucursalId(), c.getNombre(), c.getDescripcion(),
                c.getHoraDesde(), c.getHoraHasta(), c.getDiasSemana(), c.getVigenteDesde(),
                c.getVigenteHasta(), c.isEsDefault(), c.isActiva());
    }
}
