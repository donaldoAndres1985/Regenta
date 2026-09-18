package com.regenta.auditoria.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.auditoria.domain.Dispositivo;
import com.regenta.auditoria.infra.DispositivoRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * La descarga incremental de cambios del servidor (HU-104): la app pide todo
 * lo posterior a su cursor, y solo cuando confirma que terminó de procesar el
 * lote, el cursor del dispositivo avanza.
 */
@Service
public class DescargaIncremental {

    private static final int DIAS_RETENCION_POR_DEFECTO = 30; // mismo valor que el DEFAULT de politicas_retencion

    private final JdbcTemplate jdbc;
    private final DispositivoRepositorio dispositivos;

    public DescargaIncremental(JdbcTemplate jdbc, DispositivoRepositorio dispositivos) {
        this.jdbc = jdbc;
        this.dispositivos = dispositivos;
    }

    /**
     * Criterio 1: solo lo posterior al cursor. Criterio 4: un dispositivo que
     * no sincroniza hace más del período de retención se fuerza a bajar todo,
     * sin importar el cursor que pidió —lo que tenía localmente ya pudo
     * quedar incompleto frente a lo que el servidor purgó o cambió.
     */
    @Transactional(readOnly = true)
    public ResultadoDeDescarga cambiosDesde(String identificadorDispositivo, Long cursorPedido) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Optional<Dispositivo> dispositivo =
                dispositivos.findByNegocioIdAndIdentificador(negocioId, identificadorDispositivo);
        boolean forzarCompleta = dispositivo.map(d -> pasoElPeriodoDeRetencion(negocioId, d))
                .orElse(true);
        long cursor = forzarCompleta || cursorPedido == null ? 0L : cursorPedido;

        List<CambioDeServidor> cambios = jdbc.query("""
                SELECT id, entidad_tipo, entidad_id, operacion, payload::text AS payload, ocurrido_en
                FROM auditoria.cambios_servidor
                WHERE negocio_id = ? AND id > ?
                ORDER BY id
                """,
                (rs, fila) -> new CambioDeServidor(rs.getLong("id"), rs.getString("entidad_tipo"),
                        UUID.fromString(rs.getString("entidad_id")), rs.getString("operacion"),
                        rs.getString("payload"), rs.getObject("ocurrido_en", OffsetDateTime.class)),
                negocioId, cursor);

        return new ResultadoDeDescarga(cambios, forzarCompleta);
    }

    /** Criterio 2: el cursor solo avanza cuando la descarga terminó, nunca en cada pedido de lectura. */
    @Transactional
    public void confirmarDescarga(String identificadorDispositivo, long nuevoCursor) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Dispositivo dispositivo = dispositivos.findByNegocioIdAndIdentificador(negocioId,
                        identificadorDispositivo)
                .orElseThrow(() -> new NoEncontradoException("Ese dispositivo no existe"));
        dispositivo.avanzarCursor(Long.toString(nuevoCursor), OffsetDateTime.now());
        dispositivos.save(dispositivo);
    }

    private boolean pasoElPeriodoDeRetencion(UUID negocioId, Dispositivo dispositivo) {
        if (dispositivo.getUltimoSyncEn() == null) {
            return true;
        }
        int diasRetencion = diasDeRetencion(negocioId);
        return dispositivo.getUltimoSyncEn().isBefore(OffsetDateTime.now().minusDays(diasRetencion));
    }

    private int diasDeRetencion(UUID negocioId) {
        Integer dias = jdbc.query(
                "select dias_sync from auditoria.politicas_retencion where negocio_id = ?",
                rs -> rs.next() ? rs.getInt(1) : null, negocioId);
        return dias == null ? DIAS_RETENCION_POR_DEFECTO : dias;
    }
}
