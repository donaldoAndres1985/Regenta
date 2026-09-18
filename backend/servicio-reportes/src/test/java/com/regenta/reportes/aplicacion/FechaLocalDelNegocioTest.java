package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.reportes.BaseDeReportes;

/**
 * HU-097 criterio 3: el corte de los agregados diarios es a la medianoche del
 * negocio, no a la del servidor donde corre servicio-reportes.
 */
class FechaLocalDelNegocioTest extends BaseDeReportes {

    @Autowired
    private FechaLocalDelNegocio fechaLocal;
    @Autowired
    private ZonaHorariaDeNegocios zonas;

    private final UUID negocio = UUID.randomUUID();

    @Test
    @DisplayName("Sin zona configurada todavía, usa America/Bogota por defecto")
    void porDefecto() {
        // 2026-09-18T02:30:00Z son las 21:30 del 17 en Bogota (UTC-5): sigue siendo el 17.
        OffsetDateTime instante = OffsetDateTime.parse("2026-09-18T02:30:00Z");

        LocalDate fecha = enContexto(negocio, null, Set.of(), () -> fechaLocal.de(negocio, instante));

        assertThat(fecha).isEqualTo(LocalDate.of(2026, 9, 17));
    }

    @Test
    @DisplayName("El corte es a la medianoche del negocio, no la del servidor (UTC)")
    void respetaLaZonaConfigurada() {
        enContexto(negocio, null, Set.of(), () -> {
            zonas.guardar(negocio, "Pacific/Auckland");
            return null;
        });
        // En UTC todavía es 17 de septiembre a las 20:00; en Auckland (+12/+13) ya es
        // el 18. Si el corte usara la zona del servidor (o UTC), este documento
        // caería en el agregado del 17: la regla exige que caiga en el del negocio.
        OffsetDateTime instante = OffsetDateTime.parse("2026-09-17T20:00:00Z");

        LocalDate fecha = enContexto(negocio, null, Set.of(), () -> fechaLocal.de(negocio, instante));

        assertThat(fecha).isEqualTo(LocalDate.of(2026, 9, 18));
    }
}
