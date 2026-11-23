package com.regenta.facturacion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.regenta.facturacion.domain.Certificado;
import com.regenta.facturacion.domain.EstadoCertificado;
import com.regenta.facturacion.domain.OrigenDeFactura;
import com.regenta.facturacion.infrastructure.CertificadoRepositorio;
import com.regenta.facturacion.infrastructure.FacturaImpuestoRepositorio;
import com.regenta.facturacion.infrastructure.FacturaLineaRepositorio;
import com.regenta.facturacion.infrastructure.FacturaRepositorio;
import com.regenta.facturacion.infrastructure.TransmisionesLog;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-116 C4 · Sin certificado vigente no se emite, y no se gasta un consecutivo en el intento. */
class EmisionSinCertificadoTest {

    private final UUID negocio = UUID.randomUUID();
    private final CertificadoRepositorio certificados = mock(CertificadoRepositorio.class);
    private final AsignadorDeConsecutivos asignador = mock(AsignadorDeConsecutivos.class);
    private final GestionDeFacturas gestion = new GestionDeFacturas(mock(FacturaRepositorio.class),
            mock(FacturaLineaRepositorio.class), mock(FacturaImpuestoRepositorio.class), asignador,
            mock(TransmisionesLog.class), mock(CorreosDeFactura.class), mock(RegistroDeEventos.class),
            mock(AdquirienteGenerico.class), mock(EmisorDeNegocios.class), new CertificadosVigentes(certificados));

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    void c4_conElCertificadoVencidoSeRechazaAntesDelConsecutivo() {
        Certificado vencido = Certificado.registrar(negocio, "viejo", "x", "1", LocalDate.now().minusYears(2),
                LocalDate.now().minusDays(1), "ref");
        when(certificados.findByNegocioIdAndEstado(any(), any())).thenReturn(List.of(vencido));
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"),
                Set.of("FACTURACION"), Set.of(), Set.of()));

        assertThatThrownBy(() -> gestion.emitirDesdeEvento(OrigenDeFactura.VENTA, Map.of(
                "negocio_id", negocio.toString(), "venta_id", UUID.randomUUID().toString())))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("certificado");
        verifyNoInteractions(asignador);
    }

    @Test
    void c4_unoVigenteDejaPasar() {
        Certificado vigente = Certificado.registrar(negocio, "nuevo", "x", "1", LocalDate.now().minusDays(1),
                LocalDate.now().plusYears(1), "ref");
        when(certificados.findByNegocioIdAndEstado(negocio, EstadoCertificado.ACTIVO)).thenReturn(List.of(vigente));

        assertThat(new CertificadosVigentes(certificados).exigir(negocio)).isSameAs(vigente);
    }
}
