package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.reportes.BaseDeReportes;

/**
 * HU-097 criterio 2: el panel responde desde {@code agregados_diarios}, sin
 * recorrer las tablas de hechos. Se prueba insertando el agregado directo y
 * sin insertar ni una fila en hechos_venta: si el panel las necesitara para
 * responder, este test daría cero o fallaría.
 */
class PanelDiarioTest extends BaseDeReportes {

    private static final Set<String> VER_REPORTES = Set.of("REPORTES_REPORTE_VER");

    @Autowired
    private PanelDiario panel;
    @Autowired
    private ZonaHorariaDeNegocios zonas;

    private final UUID negocio = UUID.randomUUID();

    @Test
    @DisplayName("Criterio 2: el resumen de hoy sale de agregados_diarios, no de hechos_venta")
    void hoySaleDelAgregado() {
        enContexto(negocio, null, VER_REPORTES, () -> {
            zonas.guardar(negocio, "America/Bogota");
            return null;
        });
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        ejecutarComoElServicio(negocio, """
                insert into agregados_diarios (negocio_id, sucursal_id, fecha, patron, num_documentos,
                    unidades, monto_bruto, descuentos, impuestos, monto_neto, costo, margen,
                    ticket_promedio)
                values ('%s', null, '%s', 'VENTA_DIRECTA', 3, 6, 90000, 0, 0, 90000, 30000, 60000, 30000)
                """.formatted(negocio, hoy));

        ResumenDiario resumen = enContexto(negocio, null, VER_REPORTES, () -> panel.hoy());

        assertThat(resumen.documentos()).isEqualTo(3);
        assertThat(resumen.montoNeto()).isEqualByComparingTo("90000");
        assertThat(resumen.margen()).isEqualByComparingTo("60000");
        assertThat(resumen.ticketPromedio()).isEqualByComparingTo("30000");
        assertThat(contar("select count(*) from hechos_venta where negocio_id = '" + negocio + "'"))
                .as("el panel no necesitó ninguna fila de hechos_venta para responder")
                .isZero();
    }

    @Test
    @DisplayName("Sin agregados todavía, el panel responde en ceros, no en error")
    void sinDatosDevuelveCeros() {
        ResumenDiario resumen = enContexto(negocio, null, VER_REPORTES, () -> panel.hoy());

        assertThat(resumen.documentos()).isZero();
        assertThat(resumen.montoNeto()).isEqualByComparingTo("0");
        assertThat(resumen.ticketPromedio()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El mes acumula los agregados del mes en curso, no solo el de hoy")
    void mesAcumulaElMes() {
        enContexto(negocio, null, VER_REPORTES, () -> {
            zonas.guardar(negocio, "America/Bogota");
            return null;
        });
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Bogota"));
        LocalDate primeroDelMes = hoy.withDayOfMonth(1);
        for (LocalDate fecha : Set.of(hoy, primeroDelMes)) {
            ejecutarComoElServicio(negocio, """
                    insert into agregados_diarios (negocio_id, sucursal_id, fecha, patron, num_documentos,
                        unidades, monto_bruto, descuentos, impuestos, monto_neto, costo, margen,
                        ticket_promedio)
                    values ('%s', null, '%s', 'VENTA_DIRECTA', 1, 1, 10000, 0, 0, 10000, 4000, 6000, 10000)
                    on conflict (negocio_id, sucursal_key, fecha, patron) do nothing
                    """.formatted(negocio, fecha));
        }

        ResumenDiario resumen = enContexto(negocio, null, VER_REPORTES, () -> panel.mesActual());

        assertThat(resumen.documentos()).isGreaterThanOrEqualTo(1);
        assertThat(resumen.montoNeto()).isGreaterThanOrEqualTo(new java.math.BigDecimal("10000"));
    }
}
