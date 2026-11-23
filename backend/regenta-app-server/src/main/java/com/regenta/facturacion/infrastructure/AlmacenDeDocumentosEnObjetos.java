package com.regenta.facturacion.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.facturacion.application.AlmacenDeDocumentos;
import com.regenta.shared.almacen.AlmacenDeObjetos;
import com.regenta.shared.almacen.AlmacenNoDisponibleException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-121 · Los documentos de facturación (el XML firmado) en el almacén de objetos (C1). Si el
 * almacén no responde al firmar, el XML espera en {@code facturacion.documentos_por_subir} con una
 * URL provisional {@code pendiente://<id>}: la factura sigue su camino (se transmite leyendo de ahí) y
 * {@link #subirPendientes} lo sube cuando el almacén vuelve, reemplazando la URL (C3).
 */
@Component
public class AlmacenDeDocumentosEnObjetos implements AlmacenDeDocumentos {

    private static final Logger LOG = LoggerFactory.getLogger(AlmacenDeDocumentosEnObjetos.class);
    static final String PENDIENTE = "pendiente://";
    static final String CARPETA = "facturas";

    private final AlmacenDeObjetos almacen;
    private final JdbcTemplate jdbc;

    public AlmacenDeDocumentosEnObjetos(AlmacenDeObjetos almacen, JdbcTemplate jdbc) {
        this.almacen = almacen;
        this.jdbc = jdbc;
    }

    @Override
    public String guardar(UUID negocioId, String nombre, byte[] contenido, String contentType) {
        try {
            return almacen.guardar(negocioId, CARPETA, nombre, contenido, contentType);
        } catch (AlmacenNoDisponibleException caido) {
            UUID id = UUID.randomUUID();
            LOG.warn("El almacén no respondió al guardar {} del negocio {}: queda por subir ({})", nombre,
                    negocioId, caido.getMessage());
            jdbc.update("""
                    INSERT INTO facturacion.documentos_por_subir (id, negocio_id, nombre, tipo_mime, contenido,
                        intentos, ultimo_error, proximo_intento_en)
                    VALUES (?, ?, ?, ?, ?, 1, ?, now() + interval '1 minute')
                    """, id, negocioId, nombre, contentType, contenido, caido.getMessage());
            return PENDIENTE + id;
        }
    }

    @Override
    public byte[] leer(UUID negocioId, String url) {
        if (url != null && url.startsWith(PENDIENTE)) {
            UUID id = UUID.fromString(url.substring(PENDIENTE.length()));
            List<byte[]> contenido = jdbc.query(
                    "SELECT contenido FROM facturacion.documentos_por_subir WHERE id = ? AND negocio_id = ?",
                    (rs, n) -> rs.getBytes(1), id, negocioId);
            if (contenido.isEmpty()) {
                throw new NoEncontradoException("Ese documento no existe");
            }
            return contenido.get(0);
        }
        return almacen.leer(negocioId, url);
    }

    /**
     * C3 · Sube lo que quedó esperando en el negocio actual. Lo que vuelve a fallar se reintenta con una
     * espera creciente; lo subido cambia de URL en la factura y sale de la tabla. Devuelve cuántos subió.
     */
    @Transactional
    public int subirPendientes() {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<Map<String, Object>> pendientes = jdbc.queryForList("""
                SELECT id, nombre, tipo_mime, contenido, intentos FROM facturacion.documentos_por_subir
                 WHERE negocio_id = ? AND proximo_intento_en <= now()
                 ORDER BY creado_en LIMIT 50
                 FOR UPDATE SKIP LOCKED
                """, negocio);
        int subidos = 0;
        for (Map<String, Object> p : pendientes) {
            UUID id = (UUID) p.get("id");
            String provisional = PENDIENTE + id;
            try {
                String url = almacen.guardar(negocio, CARPETA, (String) p.get("nombre"), (byte[]) p.get("contenido"),
                        (String) p.get("tipo_mime"));
                jdbc.update("UPDATE facturacion.facturas SET xml_url = ? WHERE negocio_id = ? AND xml_url = ?",
                        url, negocio, provisional);
                jdbc.update("UPDATE facturacion.facturas SET pdf_url = ? WHERE negocio_id = ? AND pdf_url = ?",
                        url, negocio, provisional);
                jdbc.update("DELETE FROM facturacion.documentos_por_subir WHERE id = ?", id);
                subidos++;
            } catch (AlmacenNoDisponibleException otraVez) {
                int intentos = ((Number) p.get("intentos")).intValue() + 1;
                // 1, 4, 9… minutos, hasta una hora: sin tope de intentos, el documento no se puede perder.
                long minutos = Math.min(60, (long) intentos * intentos);
                jdbc.update("""
                        UPDATE facturacion.documentos_por_subir
                           SET intentos = ?, ultimo_error = ?, proximo_intento_en = now() + (? || ' minutes')::interval
                         WHERE id = ?
                        """, intentos, otraVez.getMessage(), String.valueOf(minutos), id);
                break; // el almacén sigue caído: el resto espera al próximo barrido
            }
        }
        return subidos;
    }
}
