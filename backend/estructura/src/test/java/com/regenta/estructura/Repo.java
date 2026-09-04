package com.regenta.estructura;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Ubica las carpetas del proyecto sin depender de desde donde se lance Maven.
 *
 * <p>Los tests de esta epica miran archivos —poms, compose, migraciones—, asi que
 * necesitan la raiz real del repo y no el directorio de trabajo del proceso.
 */
final class Repo {

    /** Los 15 servicios. El gateway va aparte porque no tiene base ni JPA. */
    static final List<String> SERVICIOS = List.of(
            "usuarios", "clientes", "inventario", "ventas", "compras",
            "recursos", "reservas", "menu", "mesas", "comandas",
            "facturacion", "caja", "alertas", "reportes", "auditoria");

    /** Esquema de cada servicio dentro de SU base. Solo dos no se llaman igual. */
    static String esquema(String servicio) {
        return switch (servicio) {
            case "usuarios" -> "core_identidad";
            case "clientes" -> "crm";
            default -> servicio;
        };
    }

    private Repo() {
    }

    /** Carpeta {@code backend/}: la que tiene el pom padre y el compose. */
    static Path backend() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null) {
            if (Files.exists(p.resolve("docker-compose.yml")) && Files.exists(p.resolve("pom.xml"))) {
                return p;
            }
            p = p.getParent();
        }
        throw new IllegalStateException("No encuentro la carpeta backend/ desde " + Paths.get("").toAbsolutePath());
    }

    /** Raiz del repositorio: la que contiene backend/, design/ y modelo-datos/. */
    static Path raiz() {
        return backend().getParent();
    }

    static Path moduloServicio(String servicio) {
        return backend().resolve("servicio-" + servicio);
    }

    static Path migracionInicial(String servicio) {
        return moduloServicio(servicio)
                .resolve("src/main/resources/db/migration/V1__esquema_inicial.sql");
    }

    static String leer(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No pude leer " + p, e);
        }
    }
}
