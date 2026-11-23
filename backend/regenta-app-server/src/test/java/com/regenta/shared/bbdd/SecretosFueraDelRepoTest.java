package com.regenta.shared.bbdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * HU-125 C4 · Ningún secreto versionado en la configuración: toda propiedad que huela a secreto (clave,
 * contraseña, llave, token, PIN) se llena desde el entorno, sin valor por defecto.
 */
class SecretosFueraDelRepoTest {

    private static final Pattern CLAVE_SECRETA = Pattern.compile(
            "^\\s*([\\w.-]*(secreto|secret|password|clave|llave|token|pin|credencial|access-key|secret-key)[\\w.-]*)\\s*:\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SOLO_ENTORNO = Pattern.compile("^\\$\\{[A-Z0-9_]+:?}$");

    @Test
    void c4_lasPropiedadesSecretasVienenSoloDelEntorno() throws IOException {
        List<String> versionados = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(Path.of("src/main/resources"))) {
            for (Path p : archivos.filter(f -> f.getFileName().toString().matches("application.*\\.(yml|yaml|properties)"))
                    .toList()) {
                for (String linea : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                    if (linea.trim().startsWith("#")) {
                        continue;
                    }
                    Matcher m = CLAVE_SECRETA.matcher(linea.replace('=', ':'));
                    if (m.matches()) {
                        String valor = m.group(3).trim().replaceAll("^\"|\"$", "");
                        if (!SOLO_ENTORNO.matcher(valor).matches()) {
                            versionados.add(p.getFileName() + ": " + m.group(1));
                        }
                    }
                }
            }
        }
        assertThat(versionados).as("propiedades secretas con valor en el repo").isEmpty();
    }

    @Test
    void elDetectorNoSeDejaEngañar() {
        assertThat(SOLO_ENTORNO.matcher("${JWT_SECRETO:}").matches()).isTrue();
        assertThat(SOLO_ENTORNO.matcher("${JWT_SECRETO}").matches()).isTrue();
        assertThat(SOLO_ENTORNO.matcher("${JWT_SECRETO:valor-por-defecto}").matches()).isFalse();
        assertThat(SOLO_ENTORNO.matcher("un-secreto-literal").matches()).isFalse();
    }
}
