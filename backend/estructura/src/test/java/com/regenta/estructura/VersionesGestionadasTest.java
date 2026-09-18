package com.regenta.estructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ninguna dependencia sin version que nadie gestione.
 *
 * <p>Este test existe porque ya paso: se declaro {@code com.nimbusds:nimbus-jose-jwt}
 * sin version dando por hecho que el BOM de Spring Boot la gestionaba, y no la
 * gestiona. Maven ni siquiera llega a compilar: no puede leer el pom.
 *
 * <p>La lista de grupos gestionados es la de los BOM que importa el pom padre
 * mas lo que el padre gestiona a mano. Un grupo que no este ahi tiene que traer
 * su version escrita.
 */
class VersionesGestionadasTest {

    private static final Pattern DEPENDENCIA =
            Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
    private static final Pattern GRUPO = Pattern.compile("<groupId>([^<]+)</groupId>");
    private static final Pattern ARTEFACTO = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private static final Pattern VERSION = Pattern.compile("<version>([^<]+)</version>");
    private static final Pattern GESTION =
            Pattern.compile("<dependencyManagement>.*?</dependencyManagement>", Pattern.DOTALL);

    /** Grupos que quedan cubiertos por un BOM o por el pom padre. */
    private static final Set<String> GESTIONADOS = Set.of(
            "com.regenta",
            "org.springframework",
            "org.springframework.boot",
            "org.springframework.cloud",
            "org.springframework.data",
            "org.springframework.security",
            "org.springdoc",
            "org.flywaydb",
            "org.postgresql",
            "org.testcontainers",
            "org.junit.jupiter",
            "org.assertj",
            "org.mockito",
            "org.aspectj",
            "org.apache.poi",
            "org.apache.pdfbox",
            "io.projectreactor",
            "com.fasterxml.jackson.core",
            "com.fasterxml.jackson.dataformat",
            "org.springframework.amqp",
            "jakarta.servlet",
            "jakarta.validation",
            "jakarta.persistence");

    @Test
    @DisplayName("Toda dependencia sin version pertenece a un grupo que alguien gestiona")
    void nadieDeclaraUnaDependenciaHuerfana() {
        List<String> huerfanas = new ArrayList<>();
        for (Path pom : poms()) {
            String contenido = GESTION.matcher(Repo.leer(pom)).replaceAll("");
            Matcher dependencias = DEPENDENCIA.matcher(contenido);
            while (dependencias.find()) {
                String bloque = dependencias.group(1);
                if (VERSION.matcher(bloque).find()) {
                    continue;
                }
                String grupo = primero(GRUPO, bloque);
                if (!GESTIONADOS.contains(grupo)) {
                    huerfanas.add(pom.getParent().getFileName() + ": " + grupo + ":"
                            + primero(ARTEFACTO, bloque) + " sin version y sin BOM que la gestione");
                }
            }
        }
        assertThat(huerfanas).isEmpty();
    }

    private static List<Path> poms() {
        List<Path> todos = new ArrayList<>();
        todos.add(Repo.backend().resolve("pom.xml"));
        todos.add(Repo.backend().resolve("comun/pom.xml"));
        todos.add(Repo.backend().resolve("gateway/pom.xml"));
        todos.add(Repo.backend().resolve("estructura/pom.xml"));
        for (String servicio : Repo.SERVICIOS) {
            todos.add(Repo.moduloServicio(servicio).resolve("pom.xml"));
        }
        return todos;
    }

    private static String primero(Pattern patron, String texto) {
        Matcher encontrado = patron.matcher(texto);
        return encontrado.find() ? encontrado.group(1) : "?";
    }
}
