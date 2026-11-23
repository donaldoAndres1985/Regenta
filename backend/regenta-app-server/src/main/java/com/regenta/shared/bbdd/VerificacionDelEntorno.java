package com.regenta.shared.bbdd;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * HU-125 C4 · Ningún secreto está en el repo: todos llegan del entorno. Esto lo hace cumplir al arrancar,
 * antes de crear cualquier bean.
 *
 * <ul>
 *   <li>En {@code REGENTA_ENTORNO=produccion}, si falta alguno (o el del JWT es corto), el arranque se
 *       detiene con la lista de variables que faltan, solo los nombres y nunca los valores. Así un
 *       despliegue mal configurado no pasa el healthcheck y la versión anterior sigue atendiendo.</li>
 *   <li>En desarrollo (el valor por defecto), el JWT se firma con una llave al azar de este arranque y la
 *       base usa la clave del contenedor local de la guía (README). Se avisa en el log.</li>
 * </ul>
 */
public class VerificacionDelEntorno implements EnvironmentPostProcessor, Ordered {

    static final String PRODUCCION = "produccion";

    /** Propiedad → variable de entorno que la llena, para el mensaje. */
    static final Map<String, String> SECRETOS_DE_PRODUCCION = new LinkedHashMap<>();

    static {
        SECRETOS_DE_PRODUCCION.put("spring.datasource.url", "DATABASE_URL");
        SECRETOS_DE_PRODUCCION.put("spring.datasource.password", "DB_PASSWORD");
        SECRETOS_DE_PRODUCCION.put("regenta.jwt.secreto", "JWT_SECRETO");
        SECRETOS_DE_PRODUCCION.put("regenta.boveda.llave-maestra", "BOVEDA_LLAVE_MAESTRA");
        SECRETOS_DE_PRODUCCION.put("regenta.almacen.s3.endpoint", "ALMACEN_S3_ENDPOINT");
        SECRETOS_DE_PRODUCCION.put("regenta.almacen.s3.access-key", "ALMACEN_S3_ACCESS_KEY");
        SECRETOS_DE_PRODUCCION.put("regenta.almacen.s3.secret-key", "ALMACEN_S3_SECRET_KEY");
        SECRETOS_DE_PRODUCCION.put("spring.mail.host", "SPRING_MAIL_HOST");
        SECRETOS_DE_PRODUCCION.put("regenta.operador.clave", "REGENTA_OPERADOR_CLAVE");
    }

    private final Log log;

    public VerificacionDelEntorno(DeferredLogFactory logs) {
        this.log = logs.getLog(VerificacionDelEntorno.class);
    }

    @Override
    public int getOrder() {
        // Después de leer application.yml y las variables de entorno.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment entorno, SpringApplication aplicacion) {
        String cual = entorno.getProperty("regenta.entorno", "desarrollo").trim().toLowerCase();
        if (PRODUCCION.equals(cual)) {
            exigir(entorno);
        } else {
            completarDesarrollo(entorno);
        }
    }

    void exigir(ConfigurableEnvironment entorno) {
        List<String> faltan = new ArrayList<>();
        SECRETOS_DE_PRODUCCION.forEach((propiedad, variable) -> {
            if (vacio(entorno.getProperty(propiedad))) {
                faltan.add(variable);
            }
        });
        String jwt = entorno.getProperty("regenta.jwt.secreto");
        if (!vacio(jwt) && jwt.trim().length() < 32) {
            faltan.add("JWT_SECRETO (mínimo 32 caracteres)");
        }
        if (!"s3".equalsIgnoreCase(entorno.getProperty("regenta.almacen.tipo", ""))) {
            faltan.add("ALMACEN_TIPO=s3 (en producción los documentos no pueden vivir en memoria)");
        }
        if (!faltan.isEmpty()) {
            throw new IllegalStateException("REGENTA_ENTORNO=produccion y faltan variables de entorno: "
                    + String.join(", ", faltan) + ". Los secretos no van en el repo: se configuran en el "
                    + "servicio (docs/migracion/07-despliegue.md).");
        }
        if (vacio(entorno.getProperty("regenta.dian.url"))) {
            log.warn("Producción sin DIAN_URL: las facturas van a la DIAN simulada. Configúrala al terminar la "
                    + "habilitación.");
        }
    }

    private void completarDesarrollo(ConfigurableEnvironment entorno) {
        Map<String, Object> locales = new LinkedHashMap<>();
        if (vacio(entorno.getProperty("regenta.jwt.secreto"))) {
            byte[] llave = new byte[48];
            new SecureRandom().nextBytes(llave);
            String secreto = Base64.getEncoder().encodeToString(llave);
            locales.put("regenta.jwt.secreto", secreto);
            locales.put("regenta.seguridad.jwt.secreto", secreto);
            log.warn("Sin JWT_SECRETO: los tokens se firman con una llave de este arranque (solo desarrollo). "
                    + "Las sesiones no sobreviven a un reinicio.");
        }
        if (vacio(entorno.getProperty("spring.datasource.password"))) {
            // La del contenedor local de la guía (README); en producción no existe este camino.
            locales.put("spring.datasource.password", "regenta");
        }
        if (!locales.isEmpty()) {
            entorno.getPropertySources().addLast(new MapPropertySource("regenta-desarrollo", locales));
        }
    }

    private static boolean vacio(String v) {
        return v == null || v.isBlank();
    }
}
