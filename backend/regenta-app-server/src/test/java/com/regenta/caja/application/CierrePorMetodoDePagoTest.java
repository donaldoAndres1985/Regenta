package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infrastructure.ArqueoDenominacionRepositorio;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.AsignadorDeConsecutivos;
import com.regenta.caja.infrastructure.CajaRepositorio;
import com.regenta.caja.infrastructure.ConfigDeCajaRepositorio;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-140 · el cierre y la consulta del arqueo por método de pago, en el caso de uso. */
class CierrePorMetodoDePagoTest {

    private final SesionDeCajaRepositorio sesiones = mock(SesionDeCajaRepositorio.class);
    private final MovimientoDeCajaRepositorio movimientos = mock(MovimientoDeCajaRepositorio.class);
    private final ArqueoDenominacionRepositorio denominaciones = mock(ArqueoDenominacionRepositorio.class);
    private final ArqueoMetodoRepositorio arqueoMetodos = mock(ArqueoMetodoRepositorio.class);
    private final ConfigDeCajaRepositorio configRepo = mock(ConfigDeCajaRepositorio.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);

    private final GestionDeConfigDeCaja config = new GestionDeConfigDeCaja(configRepo);
    private final GestionDeArqueo arqueo =
            new GestionDeArqueo(denominaciones, sesiones, movimientos, config, arqueoMetodos);
    private final GestionDeSesionesDeCaja gestion = new GestionDeSesionesDeCaja(sesiones,
            mock(CajaRepositorio.class), movimientos, mock(AsignadorDeConsecutivos.class), eventos,
            arqueo, config, arqueoMetodos);

    private final UUID negocioId = UUID.randomUUID();
    private SesionDeCaja sesion;

    @BeforeEach
    void preparar() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "EMPRESARIAL",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("CAJA_TURNO_VER", "CAJA_TURNO_EDITAR"),
                Set.of()));
        sesion = SesionDeCaja.abrir(negocioId, null, UUID.randomUUID(), "CAJA-7", UUID.randomUUID(),
                new BigDecimal("50000"));
        when(sesiones.findByIdAndNegocioId(sesion.getId(), negocioId)).thenReturn(Optional.of(sesion));
        when(configRepo.findById(negocioId)).thenReturn(Optional.empty());
        when(movimientos.totalesPorMetodo(sesion.getId())).thenReturn(List.of(
                new TotalPorMetodo(MetodoPagoCaja.EFECTIVO, new BigDecimal("150000"), null, 4),
                new TotalPorMetodo(MetodoPagoCaja.TRANSFERENCIA, new BigDecimal("30000"), null, 1),
                new TotalPorMetodo(MetodoPagoCaja.TARJETA_DEBITO, new BigDecimal("80000"), null, 2)));
        when(arqueoMetodos.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-140 C1 · Dada una sesión abierta, cuando consulto su arqueo, veo una línea por método con su esperado, sus movimientos y si es cuadrable")
    void elArqueoDeUnaSesionAbierta() {
        ArqueoPorMetodo vista = arqueo.porMetodo(sesion.getId());

        assertThat(vista.lineas()).extracting(ArqueoPorMetodo.Linea::metodo)
                .containsExactly("EFECTIVO", "TARJETA_DEBITO", "TRANSFERENCIA");
        ArqueoPorMetodo.Linea debito = vista.lineas().get(1);
        assertThat(debito.cuadrable()).isFalse();
        assertThat(debito.esperado()).isEqualByComparingTo("80000");
        assertThat(debito.movimientos()).isEqualTo(2);
    }

    @Test
    @DisplayName("HU-140 C4 · Dado un cierre sin efectivo contado ni conteo por denominaciones, se rechaza con 422")
    void sinEfectivoContadoSeRechaza() {
        when(denominaciones.existsBySesionId(sesion.getId())).thenReturn(false);

        assertThatThrownBy(() -> gestion.cerrar(sesion.getId(),
                new SolicitudDeCierre(null, null, Map.of("TRANSFERENCIA", new BigDecimal("30000")))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Debe escribir la cantidad de efectivo que hay en caja.");
        verify(sesiones, never()).save(any());
    }

    @Test
    @DisplayName("HU-140 C5 · Dado un conteo por denominaciones guardado, cuando cierro sin declarar el efectivo, el contado de efectivo es el total del conteo")
    void elEfectivoSaleDelConteoPorDenominaciones() {
        when(denominaciones.existsBySesionId(sesion.getId())).thenReturn(true);
        when(denominaciones.totalContado(sesion.getId())).thenReturn(new BigDecimal("150000"));

        gestion.cerrar(sesion.getId(),
                new SolicitudDeCierre(null, null, Map.of("TRANSFERENCIA", new BigDecimal("30000"))));

        assertThat(sesion.getMontoDeclarado()).isEqualByComparingTo("150000");
    }

    @Test
    @DisplayName("HU-140 C8 · Dada una diferencia distinta de 0, cuando cierro, la sesión queda DESCUADRADA y se publica caja_descuadrada con las líneas descuadradas")
    void descuadrePublicaLasLineasDescuadradas() {
        gestion.cerrar(sesion.getId(), new SolicitudDeCierre(new BigDecimal("150000"), null,
                Map.of("TRANSFERENCIA", new BigDecimal("20000"))));

        assertThat(sesion.quedoDescuadrada()).isTrue();
        Map<String, Object> payload = payloadDe("caja_descuadrada");
        assertThat(lineasDe(payload, "lineas_descuadradas")).singleElement()
                .satisfies(l -> {
                    assertThat(l.get("metodo")).isEqualTo("TRANSFERENCIA");
                    assertThat((BigDecimal) l.get("diferencia")).isEqualByComparingTo("-10000");
                });
    }

    @Test
    @DisplayName("HU-140 C9 · Dado cualquier cierre, cuando termina, se publica caja_cerrada con el arqueo completo")
    void todoCierrePublicaCajaCerrada() {
        gestion.cerrar(sesion.getId(), new SolicitudDeCierre(new BigDecimal("150000"), null,
                Map.of("TRANSFERENCIA", new BigDecimal("30000"))));

        assertThat(sesion.quedoDescuadrada()).isFalse();
        Map<String, Object> payload = payloadDe("caja_cerrada");
        assertThat(payload.get("estado")).isEqualTo("CUADRADA");
        assertThat(lineasDe(payload, "arqueo")).extracting(l -> l.get("metodo"))
                .containsExactlyInAnyOrder("EFECTIVO", "TRANSFERENCIA", "TARJETA_DEBITO");
        verify(eventos, never()).registrar(any(), anyString(), any(), eq("caja_descuadrada"), any());
    }

    @Test
    @DisplayName("HU-140 C10 · Dado que la configuración cambia después de un cierre, cuando consulto ese cierre, su arqueo no cambia")
    void elArqueoDeUnCierreQuedaCongelado() {
        gestion.cerrar(sesion.getId(), new SolicitudDeCierre(new BigDecimal("150000"), null,
                Map.of("TRANSFERENCIA", new BigDecimal("30000"))));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ArqueoMetodo>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(arqueoMetodos).saveAll(guardadas.capture());
        when(arqueoMetodos.findBySesionIdOrderByMetodoPago(sesion.getId()))
                .thenReturn(guardadas.getValue());

        ConfigDeCaja cambiada = ConfigDeCaja.porDefecto(negocioId);
        cambiada.fijarMetodosNoCuadrables(EnumSet.of(MetodoPagoCaja.TRANSFERENCIA));
        when(configRepo.findById(negocioId)).thenReturn(Optional.of(cambiada));

        ArqueoPorMetodo vista = arqueo.porMetodo(sesion.getId());

        ArqueoPorMetodo.Linea debito = vista.lineas().stream()
                .filter(l -> l.metodo().equals("TARJETA_DEBITO")).findFirst().orElseThrow();
        ArqueoPorMetodo.Linea transferencia = vista.lineas().stream()
                .filter(l -> l.metodo().equals("TRANSFERENCIA")).findFirst().orElseThrow();
        assertThat(debito.cuadrable()).isFalse();
        assertThat(transferencia.cuadrable()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payloadDe(String tipoEvento) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("SesionDeCaja"), eq(sesion.getId()),
                eq(tipoEvento), payload.capture());
        return (Map<String, Object>) payload.getValue();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lineasDe(Map<String, Object> payload, String clave) {
        return (List<Map<String, Object>>) payload.get(clave);
    }
}
