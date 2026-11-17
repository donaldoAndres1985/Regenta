package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/** HU-155 · unidades de medida del negocio. */
class UnidadesDelNegocioTest {

    private final UnidadMedidaRepositorio unidades = mock(UnidadMedidaRepositorio.class);
    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final UUID negocioId = UUID.randomUUID();

    private GestionDeUnidades gestion(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of(permisos), Set.of()));
        AspectJProxyFactory fabrica = new AspectJProxyFactory(new GestionDeUnidades(unidades, productos));
        fabrica.addAspect(new GuardiaDePermisos());
        return fabrica.getProxy();
    }

    @BeforeEach
    void guardar() {
        when(unidades.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-155 C1 · Se crea «GLN · Galón» que admite decimales")
    void seCrea() {
        UnidadDelNegocio u = gestion("INVENTARIO_PRODUCTO_EDITAR")
                .crear(new SolicitudDeUnidad("gln", "Galón", true));

        assertThat(u.codigo()).isEqualTo("GLN");
        assertThat(u.nombre()).isEqualTo("Galón");
        assertThat(u.permiteDecimales()).isTrue();
        assertThat(u.activa()).isTrue();
    }

    @Test
    @DisplayName("HU-155 C2 · Un código que ya existe en el negocio se rechaza con «Ya existe la unidad GLN.»")
    void codigoRepetido() {
        when(unidades.findByNegocioIdAndCodigoIgnoreCase(negocioId, "GLN"))
                .thenReturn(Optional.of(UnidadMedida.nueva(negocioId, "GLN", "Galón", true)));

        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR")
                .crear(new SolicitudDeUnidad("gln", "Galones", true)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ya existe la unidad GLN.");
    }

    @Test
    @DisplayName("HU-155 C3 · Una unidad que usan productos no se desactiva y dice cuántos la usan")
    void enUsoNoSeDesactiva() {
        UnidadMedida gln = UnidadMedida.nueva(negocioId, "GLN", "Galón", true);
        when(unidades.findById(gln.getId())).thenReturn(Optional.of(gln));
        when(productos.countByNegocioIdAndUnidadMedidaId(negocioId, gln.getId())).thenReturn(37L);

        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR").desactivar(gln.getId()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("37 productos");
        verify(unidades, never()).save(any());
    }

    @Test
    @DisplayName("HU-155 · Una unidad sin productos se desactiva")
    void sinUsoSeDesactiva() {
        UnidadMedida mts = UnidadMedida.nueva(negocioId, "MTS", "Metro", true);
        when(unidades.findById(mts.getId())).thenReturn(Optional.of(mts));

        assertThat(gestion("INVENTARIO_PRODUCTO_EDITAR").desactivar(mts.getId()).activa()).isFalse();
    }

    @Test
    @DisplayName("HU-155 · Se puede cambiar el nombre y si admite decimales")
    void seEdita() {
        UnidadMedida und = UnidadMedida.nueva(negocioId, "UND", "Unidad", false);
        when(unidades.findById(und.getId())).thenReturn(Optional.of(und));

        UnidadDelNegocio u = gestion("INVENTARIO_PRODUCTO_EDITAR")
                .editar(und.getId(), new SolicitudDeUnidad("UND", "Unidades", true));

        assertThat(u.nombre()).isEqualTo("Unidades");
        assertThat(u.permiteDecimales()).isTrue();
    }

    @Test
    @DisplayName("Unidades · Un código mal escrito se corrige al editar, en mayúsculas")
    void seCorrigeElCodigo() {
        UnidadMedida gl = UnidadMedida.nueva(negocioId, "GNL", "Galón", true);
        when(unidades.findById(gl.getId())).thenReturn(Optional.of(gl));

        UnidadDelNegocio u = gestion("INVENTARIO_PRODUCTO_EDITAR")
                .editar(gl.getId(), new SolicitudDeUnidad("gln", "Galón", true));

        assertThat(u.codigo()).isEqualTo("GLN");
    }

    @Test
    @DisplayName("Unidades · Corregir el código a uno que ya tiene otra unidad se rechaza")
    void corregirAUnCodigoAjenoSeRechaza() {
        UnidadMedida gl = UnidadMedida.nueva(negocioId, "GNL", "Galón", true);
        when(unidades.findById(gl.getId())).thenReturn(Optional.of(gl));
        when(unidades.findByNegocioIdAndCodigoIgnoreCase(negocioId, "KG"))
                .thenReturn(Optional.of(UnidadMedida.nueva(negocioId, "KG", "Kilo", true)));

        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_EDITAR")
                .editar(gl.getId(), new SolicitudDeUnidad("kg", "Galón", true)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("Ya existe la unidad KG.");
    }

    @Test
    @DisplayName("HU-155 · Sin permiso no se crea (403)")
    void sinPermiso() {
        assertThatThrownBy(() -> gestion("INVENTARIO_PRODUCTO_VER")
                .crear(new SolicitudDeUnidad("KG", "Kilo", true)))
                .isInstanceOf(SinPermisoException.class);
    }
}
