package com.regenta.facturacion.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Quién emite la factura (HU-115).
 *
 * <p>Los datos fiscales del negocio viven en la base de servicio-usuarios y
 * ningún servicio consulta la base de otro, así que aquí se guarda una copia
 * alimentada por {@code negocio_creado} y
 * {@code configuracion_negocio_actualizada}. Es el mismo patrón que la zona
 * horaria en Reportes.
 */
@Component
public class EmisorDeNegocios {

    /**
     * Lo que la DIAN exige del emisor para aceptar el documento. Sin alguno de
     * estos, la factura se rechaza: mejor no emitirla y decir qué falta.
     */
    private static final Map<String, String> OBLIGATORIOS = Map.of(
            "razon_social", "la razón social",
            "numero_documento", "el NIT",
            "direccion", "la dirección",
            "ciudad", "el municipio",
            "regimen_fiscal", "el régimen fiscal");

    private final JdbcTemplate jdbc;

    public EmisorDeNegocios(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Guarda la foto completa, no un parche: el evento trae siempre todos los
     * campos, así que un consumidor que arranca a mitad de camino queda bien
     * sin haber visto los anteriores.
     */
    @Transactional
    public void guardar(UUID negocioId, Map<String, Object> datos) {
        jdbc.update("""
                INSERT INTO facturacion.emisor_negocio (negocio_id, nombre_comercial, razon_social,
                    tipo_documento, numero_documento, digito_verificacion, direccion, ciudad,
                    departamento, codigo_postal, pais, regimen_fiscal, responsabilidades_fiscales,
                    actualizado_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now())
                ON CONFLICT (negocio_id) DO UPDATE SET
                    nombre_comercial = EXCLUDED.nombre_comercial,
                    razon_social = EXCLUDED.razon_social,
                    tipo_documento = EXCLUDED.tipo_documento,
                    numero_documento = EXCLUDED.numero_documento,
                    digito_verificacion = EXCLUDED.digito_verificacion,
                    direccion = EXCLUDED.direccion,
                    ciudad = EXCLUDED.ciudad,
                    departamento = EXCLUDED.departamento,
                    codigo_postal = EXCLUDED.codigo_postal,
                    pais = EXCLUDED.pais,
                    regimen_fiscal = EXCLUDED.regimen_fiscal,
                    responsabilidades_fiscales = EXCLUDED.responsabilidades_fiscales,
                    actualizado_en = now()
                """,
                negocioId, texto(datos.get("nombre_comercial")), texto(datos.get("razon_social")),
                texto(datos.get("tipo_documento")), texto(datos.get("numero_documento")),
                digito(datos.get("digito_verificacion")), texto(datos.get("direccion")),
                texto(datos.get("ciudad")), texto(datos.get("departamento")),
                texto(datos.get("codigo_postal")), texto(datos.get("pais")),
                texto(datos.get("regimen_fiscal")),
                responsabilidades(datos.get("responsabilidades_fiscales")));
    }

    /**
     * El snapshot que se congela con la factura. Se valida antes de devolverlo
     * y no después: emitir a medias gasta un consecutivo, y un consecutivo
     * gastado en una factura que no salió es un hueco en la numeración que hay
     * que explicarle a la DIAN.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> snapshotDe(UUID negocioId) {
        Map<String, Object> emisor = jdbc.query("""
                SELECT nombre_comercial, razon_social, tipo_documento, numero_documento,
                       digito_verificacion, direccion, ciudad, departamento, codigo_postal, pais,
                       regimen_fiscal, responsabilidades_fiscales::text AS responsabilidades
                  FROM facturacion.emisor_negocio
                 WHERE negocio_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }
                    Map<String, Object> datos = new LinkedHashMap<>();
                    datos.put("nombre_comercial", rs.getString("nombre_comercial"));
                    datos.put("razon_social", rs.getString("razon_social"));
                    datos.put("tipo_documento", rs.getString("tipo_documento"));
                    datos.put("numero_documento", rs.getString("numero_documento"));
                    datos.put("digito_verificacion", rs.getString("digito_verificacion"));
                    datos.put("direccion", rs.getString("direccion"));
                    datos.put("ciudad", rs.getString("ciudad"));
                    datos.put("departamento", rs.getString("departamento"));
                    datos.put("codigo_postal", rs.getString("codigo_postal"));
                    datos.put("pais", rs.getString("pais"));
                    datos.put("regimen_fiscal", rs.getString("regimen_fiscal"));
                    datos.put("responsabilidades_fiscales", rs.getString("responsabilidades"));
                    return datos;
                },
                negocioId);

        if (emisor == null) {
            throw new ReglaDeNegocioException("Este negocio todavía no tiene datos fiscales "
                    + "cargados: no se puede emitir");
        }
        exigirCompleto(emisor);
        return emisor;
    }

    /** El municipio del emisor, que el anexo técnico pide también para el adquiriente genérico. */
    @Transactional(readOnly = true)
    public String municipioDe(UUID negocioId) {
        return jdbc.query("SELECT ciudad FROM facturacion.emisor_negocio WHERE negocio_id = ?",
                rs -> rs.next() ? rs.getString(1) : null, negocioId);
    }

    private static void exigirCompleto(Map<String, Object> emisor) {
        List<String> faltan = new ArrayList<>();
        OBLIGATORIOS.forEach((campo, comoSeLlama) -> {
            Object valor = emisor.get(campo);
            if (valor == null || valor.toString().isBlank()) {
                faltan.add(comoSeLlama);
            }
        });
        if (!faltan.isEmpty()) {
            faltan.sort(String::compareTo);
            throw new ReglaDeNegocioException("Faltan datos fiscales del negocio para emitir: "
                    + String.join(", ", faltan));
        }
    }

    private static String texto(Object valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.toString().trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static String digito(Object valor) {
        String limpio = texto(valor);
        return limpio == null ? null : limpio.substring(0, 1);
    }

    private static String responsabilidades(Object valor) {
        if (valor instanceof List<?> lista) {
            return lista.stream()
                    .map(r -> "\"" + r.toString().replace("\"", "\\\"") + "\"")
                    .reduce((a, b) -> a + "," + b)
                    .map(unidas -> "[" + unidas + "]")
                    .orElse("[]");
        }
        return "[]";
    }
}
