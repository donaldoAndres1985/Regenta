package com.regenta.reportes.infra;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Inserta en las tablas de hechos. Van por JDBC plano, no por JPA: son tablas
 * particionadas por rango de {@code ocurrido_en} (PARTITION BY RANGE), con
 * clave primaria compuesta {@code (id, ocurrido_en)} que Postgres exige
 * justamente por la partición — mapearlo como entidad JPA con `id` autogenerado
 * más esa clave compuesta no aporta nada frente al INSERT directo.
 */
@Component
public class EscritorDeHechos {

    private final JdbcTemplate jdbc;

    public EscritorDeHechos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** HU-096 criterio 1: una fila de hecho por línea de venta, con sus dimensiones ya resueltas. */
    public void insertarVenta(UUID negocioId, int fechaId, OffsetDateTime ocurridoEn, Long sucursalSk,
            Long productoSk, Long clienteSk, Long usuarioSk, UUID ventaId, String canal,
            BigDecimal cantidad, BigDecimal montoBruto, BigDecimal descuento, BigDecimal impuesto,
            BigDecimal montoNeto, BigDecimal costo) {
        jdbc.update("""
                INSERT INTO reportes.hechos_venta (negocio_id, fecha_id, hora, ocurrido_en, sucursal_sk,
                    producto_sk, cliente_sk, usuario_sk, venta_id, canal, cantidad, monto_bruto,
                    descuento, impuesto, monto_neto, costo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                negocioId, fechaId, ocurridoEn.getHour(), ocurridoEn, sucursalSk, productoSk, clienteSk,
                usuarioSk, ventaId, canal, cantidad, montoBruto, descuento, impuesto, montoNeto, costo);
    }

    /** HU-096 criterio 4 (patrón Comanda): una fila de hecho por línea de pedido. */
    public void insertarComanda(UUID negocioId, int fechaId, OffsetDateTime ocurridoEn, Long sucursalSk,
            Long usuarioSk, UUID comandaId, UUID itemMenuId, String itemNombre, BigDecimal cantidad,
            BigDecimal montoNeto, BigDecimal costo, BigDecimal propina, Integer tiempoPreparacionMin,
            Integer tiempoMesaMin) {
        jdbc.update("""
                INSERT INTO reportes.hechos_comanda (negocio_id, fecha_id, ocurrido_en, sucursal_sk, usuario_sk,
                    comanda_id, item_menu_id, item_nombre, cantidad, monto_neto, costo, propina,
                    tiempo_preparacion_min, tiempo_mesa_min)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                negocioId, fechaId, ocurridoEn, sucursalSk, usuarioSk, comandaId, itemMenuId, itemNombre,
                cantidad, montoNeto, costo, propina, tiempoPreparacionMin, tiempoMesaMin);
    }

    /** HU-096 criterio 4 (patrón Reserva): una fila de hecho por estancia cerrada. */
    public void insertarReserva(UUID negocioId, int fechaId, OffsetDateTime ocurridoEn, Long sucursalSk,
            Long clienteSk, UUID reservaId, UUID tipoRecursoId, UUID recursoId, String estadoFinal,
            int noches, BigDecimal montoNeto, BigDecimal consumos, BigDecimal penalizacion,
            BigDecimal adr) {
        jdbc.update("""
                INSERT INTO reportes.hechos_reserva (negocio_id, fecha_id, ocurrido_en, sucursal_sk, cliente_sk,
                    reserva_id, tipo_recurso_id, recurso_id, estado_final, noches, monto_neto,
                    consumos, penalizacion, adr)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                negocioId, fechaId, ocurridoEn, sucursalSk, clienteSk, reservaId, tipoRecursoId, recursoId,
                estadoFinal, noches, montoNeto, consumos, penalizacion, adr);
    }
}
