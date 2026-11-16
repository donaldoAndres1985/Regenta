package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.Caja;
import com.regenta.caja.domain.LineaDeArqueo;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.CajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/** HU-142 · los datos de la tirilla de cierre de una sesión. */
class TirillaDeCierreTest {

    private final SesionDeCajaRepositorio sesiones = mock(SesionDeCajaRepositorio.class);
    private final CajaRepositorio cajas = mock(CajaRepositorio.class);
    private final ArqueoMetodoRepositorio arqueo = mock(ArqueoMetodoRepositorio.class);
    private final DirectorioDeUsuariosApi directorio = mock(DirectorioDeUsuariosApi.class);
    private final TirillasDeCierre tirillas = new TirillasDeCierre(sesiones, cajas, arqueo, directorio);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();
    private final UUID gerente = UUID.randomUUID();
    private Caja caja;
    private SesionDeCaja sesion;
    private List<LineaDeArqueo> lineas;

    @BeforeEach
    void unaSesionCerrada() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, gerente, "EMPRESARIAL",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("CAJA_TURNO_VER"), Set.of()));
        caja = Caja.crear(negocioId, null, "CAJA-01", "Caja Principal", null);
        sesion = SesionDeCaja.abrir(negocioId, null, caja.getId(), "CAJA-12", cajero,
                new BigDecimal("50000"));
        lineas = List.of(
                new LineaDeArqueo(MetodoPagoCaja.EFECTIVO, true, new BigDecimal("150000"),
                        new BigDecimal("145000"), 4),
                new LineaDeArqueo(MetodoPagoCaja.TARJETA_DEBITO, false, new BigDecimal("80000"),
                        new BigDecimal("80000"), 2));
        sesion.cerrarConArqueo(gerente, lineas, "Faltó un billete");

        when(sesiones.findByIdAndNegocioId(sesion.getId(), negocioId)).thenReturn(Optional.of(sesion));
        when(cajas.findByIdAndNegocioId(caja.getId(), negocioId)).thenReturn(Optional.of(caja));
        when(arqueo.findBySesionIdOrderByMetodoPago(sesion.getId())).thenReturn(
                lineas.stream().map(l -> ArqueoMetodo.de(negocioId, sesion.getId(), l)).toList());
        when(directorio.nombresDe(eq(negocioId), any())).thenReturn(
                Map.of(cajero, "Ana Gómez", gerente, "Luis Pérez"));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-142 C2 · Dada una sesión cerrada, la tirilla trae caja, número, cajeros, fechas, base, arqueo por método, diferencia total, estado y observaciones")
    void laTirillaTraeTodoElCierre() {
        TirillaDeCierre tirilla = tirillas.de(sesion.getId());

        assertThat(tirilla.caja()).isEqualTo("Caja Principal");
        assertThat(tirilla.numero()).isEqualTo("CAJA-12");
        assertThat(tirilla.cajeroApertura()).isEqualTo("Ana Gómez");
        assertThat(tirilla.cajeroCierre()).isEqualTo("Luis Pérez");
        assertThat(tirilla.cerradaEn()).isNotNull();
        assertThat(tirilla.base()).isEqualByComparingTo("50000");
        assertThat(tirilla.lineas()).extracting(ArqueoPorMetodo.Linea::metodo)
                .containsExactly("EFECTIVO", "TARJETA_DEBITO");
        assertThat(tirilla.lineas().get(1).cuadrable()).isFalse();
        assertThat(tirilla.diferenciaTotal()).isEqualByComparingTo("-5000");
        assertThat(tirilla.estado()).isEqualTo("DESCUADRADA");
        assertThat(tirilla.observaciones()).isEqualTo("Faltó un billete");
    }

    @Test
    @DisplayName("HU-142 C1 · Dada una sesión abierta, no hay tirilla de cierre: 409")
    void unaSesionAbiertaNoTieneTirilla() {
        SesionDeCaja abierta = SesionDeCaja.abrir(negocioId, null, caja.getId(), "CAJA-13", cajero,
                BigDecimal.ZERO);
        when(sesiones.findByIdAndNegocioId(abierta.getId(), negocioId)).thenReturn(Optional.of(abierta));

        assertThatThrownBy(() -> tirillas.de(abierta.getId()))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-142 C5 · Dada una sesión de otro negocio, cuando pido su tirilla, recibo 404")
    void laTirillaDeOtroNegocioNoExiste() {
        UUID ajena = UUID.randomUUID();
        when(sesiones.findByIdAndNegocioId(ajena, negocioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tirillas.de(ajena)).isInstanceOf(NoEncontradoException.class);
    }
}
