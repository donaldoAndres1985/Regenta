package com.regenta.alertas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.alertas.domain.FrecuenciaAlerta;
import com.regenta.alertas.domain.ReglaAlerta;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.infrastructure.ReglaAlertaRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-116 C3 · El certificado por vencer se avisa aunque el negocio no haya creado la regla. */
class VigilanciaDeFacturacionTest {

    private final UUID negocio = UUID.randomUUID();
    private final MotorDeAlertas motor = mock(MotorDeAlertas.class);
    private final ReglaAlertaRepositorio reglas = mock(ReglaAlertaRepositorio.class);
    private final VigilanciaDeFacturacion vigilancia = new VigilanciaDeFacturacion(motor, reglas);
    private final Map<String, Object> aviso = Map.of("negocio_id", negocio.toString(),
            "certificado_id", UUID.randomUUID().toString(), "alias", "principal",
            "vigente_hasta", "2026-12-10", "dias_para_vencer", 17);

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, null, "", "", Set.of(), Set.of("ALERTAS"),
                Set.of(), Set.of()));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    void c3_sinReglaSeCreaLaDePorDefectoYSeEvalua() {
        vigilancia.alCertificadoPorVencer(aviso);

        ArgumentCaptor<ReglaAlerta> regla = ArgumentCaptor.forClass(ReglaAlerta.class);
        verify(reglas).save(regla.capture());
        assertThat(regla.getValue().getTipoCodigo()).isEqualTo("CERTIFICADO_POR_VENCER");
        ArgumentCaptor<HechoDeAlerta> hecho = ArgumentCaptor.forClass(HechoDeAlerta.class);
        verify(motor).evaluar(hecho.capture());
        assertThat(hecho.getValue().campos())
                .containsEntry("alias", "principal")
                .containsEntry("dias_para_vencer", 17);
    }

    @Test
    void c3_siYaTieneReglaSeRespeta() {
        ReglaAlerta propia = ReglaAlerta.crear(negocio, null, "CERTIFICADO_POR_VENCER", "La mía", Map.of(),
                Severidad.ALTA, List.of("IN_APP"), List.of("CONTADOR"), List.of(), FrecuenciaAlerta.INMEDIATA, null,
                24);
        when(reglas.findByNegocioIdOrderByNombreAsc(negocio)).thenReturn(List.of(propia));

        vigilancia.alCertificadoPorVencer(aviso);

        verify(reglas, never()).save(any());
        verify(motor).evaluar(any());
    }

    @Test
    void hu117_c2_laFacturaRechazadaSeAvisaConElMotivo() {
        UUID factura = UUID.randomUUID();
        vigilancia.alFacturaRechazada(Map.of("negocio_id", negocio.toString(), "factura_id", factura.toString(),
                "numero_completo", "SETP990000123", "codigo_error", "FAK21",
                "mensaje", "Adquiriente · número de documento (datos del cliente): ID de adquiriente no Informado"));

        ArgumentCaptor<ReglaAlerta> regla = ArgumentCaptor.forClass(ReglaAlerta.class);
        verify(reglas).save(regla.capture());
        assertThat(regla.getValue().getTipoCodigo()).isEqualTo("FACTURA_RECHAZADA");
        ArgumentCaptor<HechoDeAlerta> hecho = ArgumentCaptor.forClass(HechoDeAlerta.class);
        verify(motor).evaluar(hecho.capture());
        assertThat(hecho.getValue().entidadId()).isEqualTo(factura);
        assertThat(hecho.getValue().campos()).containsEntry("numero", "SETP990000123");
        assertThat(hecho.getValue().campos().get("motivo").toString()).contains("número de documento");
    }
}
