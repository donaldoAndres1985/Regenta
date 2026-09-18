package com.regenta.reportes.aplicacion;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Resuelve el día calendario de un instante en la zona horaria DEL NEGOCIO
 * (HU-097 criterio 3): los agregados diarios cortan a la medianoche del
 * negocio, no a la del servidor donde corre este servicio.
 */
@Component
public class FechaLocalDelNegocio {

    private final ZonaHorariaDeNegocios zonas;

    public FechaLocalDelNegocio(ZonaHorariaDeNegocios zonas) {
        this.zonas = zonas;
    }

    public LocalDate de(UUID negocioId, OffsetDateTime instante) {
        ZoneId zona = ZoneId.of(zonas.de(negocioId));
        return instante.atZoneSameInstant(zona).toLocalDate();
    }
}
