package com.regenta.clientes.application;

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

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.TipoDocumento;
import com.regenta.clientes.domain.TipoPersona;
import com.regenta.clientes.infrastructure.ClienteRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ListaDePreciosInfo;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/** HU-132 · Con qué lista de precios se le vende a un cliente. */
class ListaDePreciosDelClienteTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID mayorista = UUID.randomUUID();
    private final ClienteRepositorio clientes = mock(ClienteRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final ListaDePreciosDelCliente servicio = new ListaDePreciosDelCliente(clientes, inventario);
    private Cliente cliente;

    @BeforeEach
    void preparar() {
        conPermisos("CLIENTES_CLIENTE_EDITAR");
        cliente = Cliente.crear(negocio, UUID.randomUUID(), TipoPersona.JURIDICA, TipoDocumento.NIT, "900111222", "3",
                null, null, "Ferretería El Tornillo SAS", null, null, null, "MAYORISTA", null);
        when(clientes.findByIdAndEliminadoEnIsNull(cliente.getId())).thenReturn(Optional.of(cliente));
        when(inventario.listaDePrecios(negocio, mayorista)).thenReturn(Optional.of(new ListaDePreciosInfo(mayorista,
                "Mayorista")));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, UUID.randomUUID(), "BASICO", "VENTA_DIRECTA", Set.of(),
                Set.of("CLIENTES"), Set.of(permisos), Set.of()));
    }

    @Test
    @DisplayName("Se le fija una lista vigente y se puede devolver a la del negocio")
    void fijarYQuitar() {
        assertThat(servicio.asignar(cliente.getId(), mayorista).listaPreciosId()).isEqualTo(mayorista);
        assertThat(cliente.getListaPreciosId()).isEqualTo(mayorista);

        assertThat(servicio.asignar(cliente.getId(), null).listaPreciosId()).isNull();
        assertThat(cliente.getListaPreciosId()).isNull();
    }

    @Test
    @DisplayName("Una lista que no existe o no está vigente se rechaza (422) y no se guarda")
    void listaQueNoAplica() {
        assertThatThrownBy(() -> servicio.asignar(cliente.getId(), UUID.randomUUID()))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(clientes, never()).save(any());
    }

    @Test
    @DisplayName("Un cliente de otro negocio no existe (404); sin CLIENTES_CLIENTE_EDITAR, 403")
    void otroNegocioYPermiso() {
        Cliente ajeno = Cliente.crear(UUID.randomUUID(), UUID.randomUUID(), TipoPersona.NATURAL, TipoDocumento.CC, "1",
                null, "Ana", "Ruiz", null, null, null, null, null, null);
        when(clientes.findByIdAndEliminadoEnIsNull(ajeno.getId())).thenReturn(Optional.of(ajeno));
        assertThatThrownBy(() -> servicio.asignar(ajeno.getId(), mayorista)).isInstanceOf(NoEncontradoException.class);

        AspectJProxyFactory fabrica = new AspectJProxyFactory(servicio);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        ListaDePreciosDelCliente conGuardia = fabrica.getProxy();
        conPermisos("CLIENTES_CLIENTE_VER");
        assertThatThrownBy(() -> conGuardia.asignar(cliente.getId(), mayorista)).isInstanceOf(SinPermisoException.class);
    }
}
