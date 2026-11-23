package com.regenta.facturacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-116 · Abrir el .p12 que sube el negocio. */
class CertificadoDeFirmaTest {

    @Test
    void leeEmisorSerieYVigenciaDelPropioCertificado() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2027, 6, 30);

        CertificadoDeFirma c = CertificadoDeFirma.abrir(CertificadoDePrueba.p12("s3creta", desde, hasta), "s3creta");

        assertThat(c.vigenteDesde()).isEqualTo(desde);
        assertThat(c.vigenteHasta()).isEqualTo(hasta);
        assertThat(c.emisor()).contains("CN=Negocio de Prueba");
        assertThat(c.numeroSerie()).isNotBlank();
        assertThat(c.llave()).isNotNull();
    }

    @Test
    void conLaClaveEquivocadaEs422() {
        byte[] p12 = CertificadoDePrueba.vigente("buena");

        assertThatThrownBy(() -> CertificadoDeFirma.abrir(p12, "mala"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("clave");
    }

    @Test
    void elSobreConservaArchivoYClave() {
        byte[] p12 = CertificadoDePrueba.vigente("clave con espacios: 1");

        CertificadoDeFirma c = CertificadoDeFirma.desdeSobre(CertificadoDeFirma.sobre(p12, "clave con espacios: 1"));

        assertThat(c.certificado()).isNotNull();
    }
}
