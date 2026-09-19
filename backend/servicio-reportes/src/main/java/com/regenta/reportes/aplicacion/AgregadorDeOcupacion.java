package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reparte una estancia cerrada noche por noche en {@code ocupacion_diaria}
 * (HU-099 criterio 1). Una estancia es un solo hecho, pero la ocupación es una
 * pregunta por día: alguien que entró el lunes y salió el miércoles ocupó dos
 * noches, y las dos tienen que contarse por separado.
 *
 * <p>Se acumulan las noches vendidas y el <b>ingreso de alojamiento</b>, no el
 * total de la cuenta: ADR y RevPAR miden lo que rinde la habitación. Meterles
 * el minibar y el spa las infla y deja de poder compararse con las de
 * cualquier otro hotel.
 *
 * <p>ADR y RevPAR son divisiones, y una división no se puede acumular sumando:
 * se guarda el numerador y se recalculan en cada noche que llega.
 */
@Component
public class AgregadorDeOcupacion {

    private final JdbcTemplate jdbc;

    public AgregadorDeOcupacion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @param primeraNoche la noche en que entró; se cuenta
     * @param salida       el día en que salió; NO se cuenta (esa noche ya no durmió)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void aplicar(UUID negocioId, UUID tipoRecursoId, UUID sucursalId, LocalDate primeraNoche,
            LocalDate salida, BigDecimal ingresoAlojamiento) {
        if (tipoRecursoId == null || primeraNoche == null || salida == null
                || !primeraNoche.isBefore(salida)) {
            return;
        }
        long noches = java.time.temporal.ChronoUnit.DAYS.between(primeraNoche, salida);
        BigDecimal porNoche = ingresoAlojamiento == null ? BigDecimal.ZERO
                : ingresoAlojamiento.divide(BigDecimal.valueOf(noches), 4, RoundingMode.HALF_UP);
        int activos = recursosDelTipo(negocioId, tipoRecursoId);

        for (LocalDate noche = primeraNoche; noche.isBefore(salida); noche = noche.plusDays(1)) {
            // HU-136: el denominador es lo que habia PARA VENDER esa noche, no
            // el inventario entero. Se cuenta noche por noche porque un bloqueo
            // puede tapar solo algunas de las que duro la estancia.
            int bloqueados = bloqueadosEsaNoche(negocioId, tipoRecursoId, noche);
            int totales = Math.max(activos - bloqueados, 0);
            jdbc.update("""
                    INSERT INTO reportes.ocupacion_diaria (negocio_id, sucursal_id, fecha, tipo_recurso_id,
                        recursos_totales, recursos_ocupados, recursos_bloqueados, ingreso_alojamiento,
                        adr, revpar)
                    VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, ?)
                    ON CONFLICT (negocio_id, fecha, tipo_recurso_id) DO UPDATE SET
                        recursos_totales = EXCLUDED.recursos_totales,
                        recursos_bloqueados = EXCLUDED.recursos_bloqueados,
                        recursos_ocupados = ocupacion_diaria.recursos_ocupados + 1,
                        ingreso_alojamiento = ocupacion_diaria.ingreso_alojamiento
                                              + EXCLUDED.ingreso_alojamiento,
                        adr = (ocupacion_diaria.ingreso_alojamiento + EXCLUDED.ingreso_alojamiento)
                              / (ocupacion_diaria.recursos_ocupados + 1),
                        revpar = CASE WHEN EXCLUDED.recursos_totales = 0 THEN 0
                                 ELSE (ocupacion_diaria.ingreso_alojamiento + EXCLUDED.ingreso_alojamiento)
                                      / EXCLUDED.recursos_totales END
                    """,
                    negocioId, sucursalId, noche, tipoRecursoId, totales, bloqueados, porNoche, porNoche,
                    totales == 0 ? BigDecimal.ZERO
                            : porNoche.divide(BigDecimal.valueOf(totales), 4, RoundingMode.HALF_UP));
        }
    }

    /**
     * Recalcula el denominador de una noche sin tocar lo vendido (HU-136). La
     * usa el consumidor de bloqueos: un bloqueo que llega despues de la
     * estancia tiene que corregir la fila que ya existe, y uno que llega antes
     * tiene que crearla —si no, una noche entera en obra sin reservas no
     * aparece en ningun reporte y no hay como explicar el hueco.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recalcularNoche(UUID negocioId, UUID tipoRecursoId, UUID sucursalId, LocalDate noche) {
        if (tipoRecursoId == null || noche == null) {
            return;
        }
        int bloqueados = bloqueadosEsaNoche(negocioId, tipoRecursoId, noche);
        int totales = Math.max(recursosDelTipo(negocioId, tipoRecursoId) - bloqueados, 0);
        jdbc.update("""
                INSERT INTO reportes.ocupacion_diaria (negocio_id, sucursal_id, fecha, tipo_recurso_id,
                    recursos_totales, recursos_ocupados, recursos_bloqueados, ingreso_alojamiento,
                    adr, revpar)
                VALUES (?, ?, ?, ?, ?, 0, ?, 0, NULL, NULL)
                ON CONFLICT (negocio_id, fecha, tipo_recurso_id) DO UPDATE SET
                    recursos_totales = EXCLUDED.recursos_totales,
                    recursos_bloqueados = EXCLUDED.recursos_bloqueados,
                    revpar = CASE WHEN EXCLUDED.recursos_totales = 0 THEN 0
                             ELSE ocupacion_diaria.ingreso_alojamiento / EXCLUDED.recursos_totales END
                """,
                negocioId, sucursalId, noche, tipoRecursoId, totales, bloqueados);
    }

    private int bloqueadosEsaNoche(UUID negocioId, UUID tipoRecursoId, LocalDate noche) {
        Integer cuantos = jdbc.queryForObject(
                "SELECT count(*) FROM reportes.bloqueos_recurso_dia WHERE negocio_id = ? "
                        + "AND tipo_recurso_id = ? AND fecha = ?",
                Integer.class, negocioId, tipoRecursoId, noche);
        return cuantos == null ? 0 : cuantos;
    }

    /**
     * El inventario de hoy, no el del día de la estancia: {@code dim_recurso}
     * no está versionada. Un hotel que agrega un ala nueva recalcula hacia
     * atrás la ocupación de los meses viejos, y eso está anotado en la V4.
     */
    private int recursosDelTipo(UUID negocioId, UUID tipoRecursoId) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM reportes.dim_recurso WHERE negocio_id = ? "
                        + "AND tipo_recurso_id = ? AND activo",
                Integer.class, negocioId, tipoRecursoId);
        return total == null ? 0 : total;
    }
}
