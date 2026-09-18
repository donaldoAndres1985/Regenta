package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * El único punto de escritura de {@code agregados_diarios} (HU-097). Lo
 * llaman los consumidores de cierre de documento —{@code venta_completada},
 * {@code pedido_completado}, {@code estancia_finalizada}— al aplicar
 * (criterio 1), y el de anulación al revertir (criterio 4).
 *
 * <p>{@code agregados_diarios_documento} guarda lo que cada documento sumó:
 * sin eso, revertir no sabría cuánto restar, y aplicar el mismo documento dos
 * veces —el mismo reproceso que ya cubre el Inbox del consumidor, pero una
 * segunda red aquí no sobra— no duplicaría el total.
 */
@Component
public class AgregadorDiario {

    private final JdbcTemplate jdbc;

    public AgregadorDiario(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void aplicar(UUID negocioId, String docTipo, UUID docId, UUID sucursalId, LocalDate fecha,
            String patron, BigDecimal unidades, BigDecimal montoBruto, BigDecimal descuentos,
            BigDecimal impuestos, BigDecimal montoNeto, BigDecimal costo, UUID clienteId) {
        int filas = jdbc.update("""
                INSERT INTO reportes.agregados_diarios_documento (negocio_id, doc_tipo, doc_id, sucursal_id,
                    fecha, patron, unidades, monto_bruto, descuentos, impuestos, monto_neto, costo,
                    cliente_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (negocio_id, doc_tipo, doc_id) DO NOTHING
                """,
                negocioId, docTipo, docId, sucursalId, fecha, patron, unidades, montoBruto, descuentos,
                impuestos, montoNeto, costo, clienteId);
        if (filas == 0) {
            return;
        }
        upsertar(negocioId, sucursalId, fecha, patron, 1, unidades, montoBruto, descuentos, impuestos,
                montoNeto, costo);
    }

    @Transactional
    public void revertir(UUID negocioId, String docTipo, UUID docId) {
        List<Map<String, Object>> filas = jdbc.queryForList("""
                DELETE FROM reportes.agregados_diarios_documento
                WHERE negocio_id = ? AND doc_tipo = ? AND doc_id = ?
                RETURNING sucursal_id, fecha, patron, unidades, monto_bruto, descuentos, impuestos,
                    monto_neto, costo
                """,
                negocioId, docTipo, docId);
        if (filas.isEmpty()) {
            return;
        }
        Map<String, Object> fila = filas.get(0);
        upsertar(negocioId, (UUID) fila.get("sucursal_id"), ((java.sql.Date) fila.get("fecha")).toLocalDate(),
                (String) fila.get("patron"), -1,
                negar((BigDecimal) fila.get("unidades")), negar((BigDecimal) fila.get("monto_bruto")),
                negar((BigDecimal) fila.get("descuentos")), negar((BigDecimal) fila.get("impuestos")),
                negar((BigDecimal) fila.get("monto_neto")), negar((BigDecimal) fila.get("costo")));
    }

    private void upsertar(UUID negocioId, UUID sucursalId, LocalDate fecha, String patron, int documentos,
            BigDecimal unidades, BigDecimal montoBruto, BigDecimal descuentos, BigDecimal impuestos,
            BigDecimal montoNeto, BigDecimal costo) {
        BigDecimal margen = montoNeto.subtract(costo);
        BigDecimal ticketPromedio = documentos == 0 ? BigDecimal.ZERO
                : montoNeto.divide(BigDecimal.valueOf(documentos), 4, RoundingMode.HALF_UP);
        jdbc.update("""
                INSERT INTO reportes.agregados_diarios (negocio_id, sucursal_id, fecha, patron,
                    num_documentos, unidades, monto_bruto, descuentos, impuestos, monto_neto, costo, margen,
                    ticket_promedio)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (negocio_id, sucursal_key, fecha, patron) DO UPDATE SET
                    num_documentos  = agregados_diarios.num_documentos + EXCLUDED.num_documentos,
                    unidades        = agregados_diarios.unidades + EXCLUDED.unidades,
                    monto_bruto     = agregados_diarios.monto_bruto + EXCLUDED.monto_bruto,
                    descuentos      = agregados_diarios.descuentos + EXCLUDED.descuentos,
                    impuestos       = agregados_diarios.impuestos + EXCLUDED.impuestos,
                    monto_neto      = agregados_diarios.monto_neto + EXCLUDED.monto_neto,
                    costo           = agregados_diarios.costo + EXCLUDED.costo,
                    margen          = agregados_diarios.margen + EXCLUDED.margen,
                    ticket_promedio = CASE
                        WHEN agregados_diarios.num_documentos + EXCLUDED.num_documentos = 0 THEN 0
                        ELSE (agregados_diarios.monto_neto + EXCLUDED.monto_neto)
                             / (agregados_diarios.num_documentos + EXCLUDED.num_documentos)
                        END,
                    actualizado_en  = now()
                """,
                negocioId, sucursalId, fecha, patron, documentos, unidades, montoBruto, descuentos,
                impuestos, montoNeto, costo, margen, ticketPromedio);
    }

    private static BigDecimal negar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.negate();
    }
}
