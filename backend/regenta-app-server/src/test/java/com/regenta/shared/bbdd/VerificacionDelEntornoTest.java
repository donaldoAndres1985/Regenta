package com.regenta.shared.bbdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.mock.env.MockEnvironment;

/** HU-125 C4 · En producción, sin sus secretos no arranca; en desarrollo arranca sin pedir nada. */
class VerificacionDelEntornoTest {

    private final VerificacionDelEntorno verificacion = new VerificacionDelEntorno(new DeferredLogs());

    private static MockEnvironment produccionCompleta() {
        return new MockEnvironment()
                .withProperty("regenta.entorno", "produccion")
                .withProperty("spring.datasource.url", "jdbc:postgresql://db/regenta")
                .withProperty("spring.datasource.password", "x")
                .withProperty("regenta.jwt.secreto", "a".repeat(40))
                .withProperty("regenta.boveda.llave-maestra", "llave")
                .withProperty("regenta.almacen.tipo", "s3")
                .withProperty("regenta.almacen.s3.endpoint", "https://r2")
                .withProperty("regenta.almacen.s3.access-key", "ak")
                .withProperty("regenta.almacen.s3.secret-key", "sk")
                .withProperty("spring.mail.host", "smtp")
                .withProperty("regenta.operador.clave", "operador");
    }

    @Test
    void c4_produccionCompletaArranca() {
        verificacion.postProcessEnvironment(produccionCompleta(), null);
    }

    @Test
    void c4_produccionSinSecretosNoArrancaYDiceCualesFaltanSinMostrarValores() {
        MockEnvironment entorno = produccionCompleta()
                .withProperty("spring.datasource.password", "")
                .withProperty("regenta.boveda.llave-maestra", " ")
                .withProperty("regenta.jwt.secreto", "corto-y-secreto");

        assertThatThrownBy(() -> verificacion.postProcessEnvironment(entorno, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_PASSWORD")
                .hasMessageContaining("BOVEDA_LLAVE_MAESTRA")
                .hasMessageContaining("JWT_SECRETO (mínimo 32 caracteres)")
                .hasMessageNotContaining("corto-y-secreto");
    }

    @Test
    void c4_enProduccionLosDocumentosNoVivenEnMemoria() {
        MockEnvironment entorno = produccionCompleta().withProperty("regenta.almacen.tipo", "memoria");

        assertThatThrownBy(() -> verificacion.postProcessEnvironment(entorno, null))
                .hasMessageContaining("ALMACEN_TIPO=s3");
    }

    @Test
    void enDesarrolloSinSecretosArrancaConValoresLocales() {
        MockEnvironment entorno = new MockEnvironment();

        verificacion.postProcessEnvironment(entorno, null);

        String jwt = entorno.getProperty("regenta.jwt.secreto");
        assertThat(jwt).hasSizeGreaterThanOrEqualTo(32);
        assertThat(entorno.getProperty("regenta.seguridad.jwt.secreto")).isEqualTo(jwt);
        assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("regenta");
    }

    @Test
    void enDesarrolloLlenaLasClavesQueApplicationYmlDejaVacias() {
        // Como en application.yml: regenta.jwt.secreto: ${JWT_SECRETO:} sin la variable resuelve a "".
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("regenta.jwt.secreto", "")
                .withProperty("regenta.seguridad.jwt.secreto", "")
                .withProperty("spring.datasource.password", "");

        verificacion.postProcessEnvironment(entorno, null);

        assertThat(entorno.getProperty("regenta.jwt.secreto")).hasSizeGreaterThanOrEqualTo(32);
        assertThat(entorno.getProperty("regenta.seguridad.jwt.secreto"))
                .isEqualTo(entorno.getProperty("regenta.jwt.secreto"));
        assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("regenta");
    }

    @Test
    void enDesarrolloLoQueVieneDelEntornoManda() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty("regenta.jwt.secreto", "b".repeat(40))
                .withProperty("spring.datasource.password", "propia");

        verificacion.postProcessEnvironment(entorno, null);

        assertThat(entorno.getProperty("regenta.jwt.secreto")).isEqualTo("b".repeat(40));
        assertThat(entorno.getProperty("spring.datasource.password")).isEqualTo("propia");
    }
}
