package com.regenta.estructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * HU-001 . Scaffolding del monorepo de backend.
 *
 * <p>El criterio 1 —que compilen los 16 modulos— lo prueba el propio build: si un
 * modulo no compila, estos tests no llegan a correr. Lo que se verifica aqui es
 * que la estructura sea la acordada y siga siendolo dentro de seis meses.
 */
class EsqueletoBackendTest {

    private static Document xml(Path pom) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return db.parse(pom.toFile());
    }

    private static List<String> textos(Document doc, String tag) {
        NodeList nl = doc.getElementsByTagName(tag);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            out.add(nl.item(i).getTextContent().trim());
        }
        return out;
    }

    @Test
    @DisplayName("criterio 1 . el pom padre declara el gateway y los 15 servicios")
    void modulosDeclarados() throws Exception {
        List<String> modulos = textos(xml(Repo.backend().resolve("pom.xml")), "module");

        assertThat(modulos).contains("gateway");
        for (String s : Repo.SERVICIOS) {
            assertThat(modulos).contains("servicio-" + s);
            assertThat(Repo.moduloServicio(s).resolve("pom.xml")).exists();
        }
        assertThat(Files.exists(Repo.backend().resolve("gateway/pom.xml"))).isTrue();
    }

    @Test
    @DisplayName("criterio 2 . las versiones se declaran una sola vez, en el padre")
    void versionesSoloEnElPadre() throws Exception {
        Document padre = xml(Repo.backend().resolve("pom.xml"));

        assertThat(textos(padre, "java.version")).containsExactly("17");
        assertThat(padre.getElementsByTagName("dependencyManagement").getLength()).isEqualTo(1);
        assertThat(textos(padre, "artifactId"))
                .contains("spring-boot-dependencies", "spring-cloud-dependencies");

        // Ningun modulo hijo fija versiones: si una se cuela aqui, dentro de un ano
        // hay dos versiones de la misma libreria en el mismo despliegue.
        List<Path> hijos = new ArrayList<>();
        hijos.add(Repo.backend().resolve("gateway/pom.xml"));
        hijos.add(Repo.backend().resolve("comun/pom.xml"));
        hijos.add(Repo.backend().resolve("estructura/pom.xml"));
        Repo.SERVICIOS.forEach(s -> hijos.add(Repo.moduloServicio(s).resolve("pom.xml")));

        for (Path pom : hijos) {
            Document doc = xml(pom);
            NodeList deps = doc.getElementsByTagName("dependency");
            for (int i = 0; i < deps.getLength(); i++) {
                Element dep = (Element) deps.item(i);
                assertThat(dep.getElementsByTagName("version").getLength())
                        .as("version fijada a mano en %s . %s", pom.getParent().getFileName(),
                                dep.getElementsByTagName("artifactId").item(0).getTextContent())
                        .isZero();
            }
            // Los hijos heredan del padre, no de spring-boot-starter-parent.
            Node parent = doc.getElementsByTagName("parent").item(0);
            assertThat(parent).as("falta <parent> en %s", pom).isNotNull();
            assertThat(((Element) parent).getElementsByTagName("artifactId").item(0).getTextContent())
                    .isEqualTo("regenta-backend");
        }
    }

    @Test
    @DisplayName("criterio 2 . el compilador esta fijado y compila contra Java 17")
    void compiladorFijado() throws Exception {
        // Sin version fija, Maven usa la de su super-POM. En instalaciones viejas eso
        // es el compilador 3.1, que no entiende <release> y compila contra Java 5:
        // "Source option 5 is no longer supported". El build pasa o falla segun la
        // maquina, que es la peor forma de fallar.
        String padre = Repo.leer(Repo.backend().resolve("pom.xml"));

        assertThat(padre).contains("maven-compiler-plugin");
        assertThat(padre).contains("<maven-compiler-plugin.version>");
        assertThat(padre).contains("<release>${java.version}</release>");
        assertThat(padre).contains("maven-surefire-plugin");
        assertThat(padre).contains("<maven-surefire-plugin.version>");

        // Maven puede correr sobre un JDK mas viejo que el del proyecto. El
        // toolchain hace que se compile con 21 igual, o que falle diciendolo.
        assertThat(padre).contains("maven-toolchains-plugin");
        assertThat(padre).contains("select-jdk-toolchain");
        assertThat(padre).contains("<version>[${java.version},)</version>");

        // Sin esto, Spring pierde los nombres de los parametros y no puede elegir
        // entre dos beans del mismo tipo. Lo trae starter-parent, del que este POM
        // no hereda a proposito.
        assertThat(padre).contains("<parameters>true</parameters>");
    }

    @Test
    @DisplayName("criterio 3 . los paquetes son por feature, no por capa")
    void paquetesPorFeature() {
        for (String s : Repo.SERVICIOS) {
            Path base = Repo.moduloServicio(s).resolve("src/main/java/com/regenta/" + s);
            assertThat(base).as("falta el paquete raiz de %s", s).exists();
            for (String paquete : List.of("domain", "api", "infra")) {
                assertThat(base.resolve(paquete)).as("falta %s.%s", s, paquete).exists();
            }
            // Por capa seria com.regenta.domain.<servicio>: eso no debe existir.
            Path porCapa = Repo.moduloServicio(s).resolve("src/main/java/com/regenta/domain");
            assertThat(Files.exists(porCapa)).as("%s esta organizado por capa", s).isFalse();
        }
    }

    @Test
    @DisplayName("criterio 4 . ningun archivo de contexto de IA queda versionado")
    void sinArchivosDeContextoDeIa() throws Exception {
        List<String> prohibidos = List.of(
                "CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "GEMINI.md", ".cursorrules");

        String gitignore = Repo.leer(Repo.raiz().resolve(".gitignore"));
        for (String archivo : prohibidos) {
            assertThat(gitignore).as("%s no esta en .gitignore", archivo).contains(archivo);
        }
        assertThat(gitignore).contains(".claude/");

        try (var paths = Files.walk(Repo.backend())) {
            List<Path> encontrados = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> prohibidos.contains(p.getFileName().toString()))
                    .toList();
            assertThat(encontrados).as("hay archivos de contexto de IA dentro de backend/").isEmpty();
        }
    }
}
