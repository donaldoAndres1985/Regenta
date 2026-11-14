package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.caja.application.GestionDeConfigDeCaja.ConfigCajaDelNegocio;
import com.regenta.caja.infrastructure.ConfigDeCajaRepositorio;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/** HU-139 · caso de uso de la configuración de métodos no cuadrables. */
class GestionDeConfigDeCajaTest {

    private final ConfigDeCajaRepositorio repositorio = mock(ConfigDeCajaRepositorio.class);
    private final UUID negocioId = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-139 C2 · Dado un usuario con CAJA_TURNO_EDITAR, cuando cambia la lista, queda guardada y la devuelve la respuesta")
    void conPermisoLaListaQuedaGuardada() {
        conPermisos("CAJA_TURNO_VER", "CAJA_TURNO_EDITAR");
        when(repositorio.findById(negocioId)).thenReturn(Optional.empty());
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfigCajaDelNegocio respuesta = protegida().fijarMetodosNoCuadrables(
                new SolicitudDeMetodosNoCuadrables(List.of("BONO", "CREDITO")));

        assertThat(respuesta.metodosNoCuadrables()).containsExactlyInAnyOrder("BONO", "CREDITO");
        verify(repositorio).save(any());
    }

    @Test
    @DisplayName("HU-139 C3 · Dado un usuario sin CAJA_TURNO_EDITAR, cuando intenta cambiarla, recibe 403")
    void sinPermisoSeRechaza() {
        conPermisos("CAJA_TURNO_VER");

        assertThatThrownBy(() -> protegida().fijarMetodosNoCuadrables(
                new SolicitudDeMetodosNoCuadrables(List.of("BONO"))))
                .isInstanceOf(SinPermisoException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("HU-139 C5 · Dada una lista con un método que no existe, cuando la guardo, se rechaza con 422")
    void unMetodoDesconocidoSeRechaza() {
        conPermisos("CAJA_TURNO_VER", "CAJA_TURNO_EDITAR");
        when(repositorio.findById(negocioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> protegida().fijarMetodosNoCuadrables(
                new SolicitudDeMetodosNoCuadrables(List.of("BONO", "CHEQUE"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("CHEQUE");
        verify(repositorio, never()).save(any());
    }

    /** El servicio con la guardia de permisos real delante, como lo deja Spring. */
    private GestionDeConfigDeCaja protegida() {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(new GestionDeConfigDeCaja(repositorio));
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        return fabrica.getProxy();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "EMPRESARIAL",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(permisos), Set.of()));
    }
}
