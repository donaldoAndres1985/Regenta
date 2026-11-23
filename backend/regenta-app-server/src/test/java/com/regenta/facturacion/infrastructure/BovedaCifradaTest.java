package com.regenta.facturacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.regenta.shared.almacen.AlmacenEnMemoria;
import com.regenta.shared.web.NoEncontradoException;

/** HU-116 C1 y C5 · La bóveda cifra por negocio y un negocio no lee lo de otro. */
class BovedaCifradaTest {

    private static final String MAESTRA = llave();
    private final AlmacenEnMemoria almacen = new AlmacenEnMemoria();
    private final BovedaCifrada boveda = new BovedaCifrada(almacen, MAESTRA);
    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final byte[] secreto = "llave privada del negocio A".getBytes(StandardCharsets.UTF_8);

    @Test
    void c1_loGuardadoEstaCifradoYSeDevuelveIgual() {
        String referencia = boveda.guardar(negocioA, "certificado-principal", secreto);

        byte[] enElAlmacen = almacen.leer(negocioA, referencia);
        assertThat(new String(enElAlmacen, StandardCharsets.ISO_8859_1))
                .doesNotContain("llave privada");
        assertThat(boveda.leer(negocioA, referencia)).isEqualTo(secreto);
    }

    @Test
    void c1_conOtraLlaveMaestraNoSeDescifra() {
        String referencia = boveda.guardar(negocioA, "certificado", secreto);
        BovedaCifrada otraMaestra = new BovedaCifrada(almacen, llave());

        assertThatThrownBy(() -> otraMaestra.leer(negocioA, referencia))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void c5_otroNegocioNoEncuentraLaReferenciaAjena() {
        String referencia = boveda.guardar(negocioA, "certificado", secreto);

        assertThatThrownBy(() -> boveda.leer(negocioB, referencia)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void c5_aunqueCopiaraElCifradoAjenoNoLoDescifra() {
        byte[] sobreDeA = boveda.cifrar(negocioA, secreto);

        assertThatThrownBy(() -> boveda.descifrar(negocioB, sobreDeA))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(boveda.descifrar(negocioA, sobreDeA)).isEqualTo(secreto);
    }

    @Test
    void unSobreAlteradoNoSeDescifra() {
        byte[] sobre = boveda.cifrar(negocioA, secreto);
        sobre[sobre.length - 1] ^= 1;

        assertThatThrownBy(() -> boveda.descifrar(negocioA, sobre)).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    void unaLlaveMaestraQueNoEsDe32BytesNoArranca() {
        String corta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new BovedaCifrada(almacen, corta)).isInstanceOf(IllegalStateException.class);
    }

    private static String llave() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }
}
