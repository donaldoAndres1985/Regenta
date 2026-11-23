package com.regenta.auditoria.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.almacen.AlmacenDeObjetos;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/**
 * HU-131 · Archiva la bitácora vencida de un negocio. Las particiones de {@code eventos_auditoria}
 * son compartidas entre negocios, así que no se suelta una partición: se toman los eventos de ESTE
 * negocio que pasaron su retención (C1), un mes por archivo, se escriben como JSON por línea
 * comprimido en el almacén de objetos (HU-121), se anota el archivo en
 * {@code auditoria.archivos_auditoria} y se borran de la tabla viva por id. Los de otros negocios en la
 * misma partición no se tocan (C2).
 *
 * <p>El orden importa: primero el archivo en el almacén, luego su fila y por último el borrado, todo en
 * la misma transacción. El trigger de V4 solo deja borrar lo que un archivo ya cubre. Si el almacén no
 * responde no se borra nada y el barrido lo intenta otra vez.
 */
@Component
public class ArchivadorDeBitacora {

    private static final Logger LOG = LoggerFactory.getLogger(ArchivadorDeBitacora.class);
    static final String CARPETA = "bitacora";

    /** Meses por pasada: lo que falte lo toma el próximo barrido. */
    private static final int MESES_POR_PASADA = 6;

    private final JdbcTemplate jdbc;
    private final AlmacenDeObjetos almacen;
    private final RetencionDeBitacora retencion;

    public ArchivadorDeBitacora(JdbcTemplate jdbc, AlmacenDeObjetos almacen, RetencionDeBitacora retencion) {
        this.jdbc = jdbc;
        this.almacen = almacen;
        this.retencion = retencion;
    }

    /** Lo que se archivó en una pasada. */
    public record Archivado(UUID archivoId, OffsetDateTime desde, OffsetDateTime hasta, int eventos, String url) {
    }

    /** Barrido: archiva lo vencido del negocio actual y devuelve cuántos eventos salieron de la tabla viva. */
    @Transactional
    public int archivarVencidos() {
        return archivarVencidos(OffsetDateTime.now(ZoneOffset.UTC)).stream().mapToInt(Archivado::eventos).sum();
    }

    /** {@code ahora} para las pruebas: el límite es {@code ahora − retención}. */
    @Transactional
    public List<Archivado> archivarVencidos(OffsetDateTime ahora) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        int dias = retencion.de(negocio).dias();
        OffsetDateTime limite = ahora.minusDays(dias);

        List<Archivado> hechos = new ArrayList<>();
        for (int i = 0; i < MESES_POR_PASADA; i++) {
            OffsetDateTime masViejo = jdbc.queryForObject(
                    "SELECT min(ocurrido_en) FROM auditoria.eventos_auditoria WHERE negocio_id = ? AND ocurrido_en < ?",
                    OffsetDateTime.class, negocio, limite);
            if (masViejo == null) {
                break;
            }
            OffsetDateTime inicioDelMes = masViejo.withOffsetSameInstant(ZoneOffset.UTC).withDayOfMonth(1)
                    .toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime finDelMes = inicioDelMes.plusMonths(1);
            OffsetDateTime hasta = finDelMes.isBefore(limite) ? finDelMes : limite;
            hechos.add(archivar(negocio, inicioDelMes, hasta));
        }
        if (!hechos.isEmpty()) {
            LOG.info("Bitácora del negocio {}: {} eventos archivados en {} archivos (retención {} días)", negocio,
                    hechos.stream().mapToInt(Archivado::eventos).sum(), hechos.size(), dias);
        }
        return hechos;
    }

    /** Un mes (o lo vencido de él) del negocio: al almacén, anotado y fuera de la tabla viva. */
    private Archivado archivar(UUID negocio, OffsetDateTime desde, OffsetDateTime hastaExclusivo) {
        List<String[]> filas = jdbc.query("""
                SELECT e.id::text, row_to_json(e)::text
                  FROM auditoria.eventos_auditoria e
                 WHERE e.negocio_id = ? AND e.ocurrido_en >= ? AND e.ocurrido_en < ?
                 ORDER BY e.ocurrido_en, e.id
                """, (rs, n) -> new String[] {rs.getString(1), rs.getString(2)}, negocio, desde, hastaExclusivo);
        OffsetDateTime[] rango = jdbc.queryForObject("""
                SELECT min(ocurrido_en), max(ocurrido_en) FROM auditoria.eventos_auditoria
                 WHERE negocio_id = ? AND ocurrido_en >= ? AND ocurrido_en < ?
                """, (rs, n) -> new OffsetDateTime[] {rs.getObject(1, OffsetDateTime.class),
                    rs.getObject(2, OffsetDateTime.class)}, negocio, desde, hastaExclusivo);

        byte[] contenido = comprimir(filas);
        String nombre = String.format("bitacora-%04d-%02d.jsonl.gz", desde.getYear(), desde.getMonthValue());
        String url = almacen.guardar(negocio, CARPETA, nombre, contenido, "application/gzip");

        UUID archivoId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO auditoria.archivos_auditoria (id, negocio_id, desde, hasta, eventos, url)
                VALUES (?, ?, ?, ?, ?, ?)
                """, archivoId, negocio, Timestamp.from(rango[0].toInstant()), Timestamp.from(rango[1].toInstant()),
                filas.size(), url);
        // Por id: solo lo que quedó en el archivo, y solo de este negocio (C2).
        UUID[] ids = filas.stream().map(f -> UUID.fromString(f[0])).toArray(UUID[]::new);
        jdbc.update("""
                DELETE FROM auditoria.eventos_auditoria
                 WHERE negocio_id = ? AND ocurrido_en >= ? AND ocurrido_en < ? AND id = ANY (?)
                """, ps -> {
                    ps.setObject(1, negocio);
                    ps.setObject(2, desde);
                    ps.setObject(3, hastaExclusivo);
                    ps.setArray(4, ps.getConnection().createArrayOf("uuid", ids));
                });
        return new Archivado(archivoId, rango[0], rango[1], filas.size(), url);
    }

    /** Un evento por línea, como JSON con todas sus columnas, comprimido. */
    static byte[] comprimir(List<String[]> filas) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(salida)) {
            for (String[] f : filas) {
                gzip.write(f[1].getBytes(StandardCharsets.UTF_8));
                gzip.write('\n');
            }
            gzip.finish();
            return salida.toByteArray();
        } catch (IOException fallo) {
            throw new UncheckedIOException(fallo);
        }
    }
}
