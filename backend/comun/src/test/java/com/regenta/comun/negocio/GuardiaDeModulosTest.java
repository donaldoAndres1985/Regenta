package com.regenta.comun.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.comun.errores.ModuloNoIncluidoException;

/** HU-017. El plan se comprueba en el backend, no ocultando el boton. */
class GuardiaDeModulosTest {

    /** Un servicio cualquiera de los que se anotan en los quince modulos. */
    static class Facturador {

        @RequiereModulo("FACTURACION")
        String emitir() {
            return "factura emitida";
        }

        String consultar() {
            return "consulta libre";
        }
    }

    private Facturador conGuardia() {
        AspectJProxyFactory fabrica = new AspectJProxyFactory(new Facturador());
        fabrica.addAspect(new GuardiaDeModulos());
        return fabrica.getProxy();
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("Criterio 1: sin el modulo en el plan responde 402, no 403")
    void sinElModuloCorta() {
        ContextoDeNegocio.establecer(conModulos(Set.of("VENTAS", "INVENTARIO")));

        assertThatThrownBy(() -> conGuardia().emitir())
                .isInstanceOf(ModuloNoIncluidoException.class)
                .satisfies(fallo -> assertThat(((ModuloNoIncluidoException) fallo).getEstado().value())
                        .isEqualTo(402))
                .hasMessageContaining("FACTURACION");
    }

    @Test
    @DisplayName("Criterio 2: con el modulo activo pasa, venga del plan o de un add-on")
    void conElModuloPasa() {
        ContextoDeNegocio.establecer(conModulos(Set.of("VENTAS", "FACTURACION")));

        assertThat(conGuardia().emitir()).isEqualTo("factura emitida");
    }

    @Test
    @DisplayName("Lo que no esta anotado no se toca")
    void loNoAnotadoNoSeToca() {
        ContextoDeNegocio.establecer(conModulos(Set.of()));

        assertThat(conGuardia().consultar()).isEqualTo("consulta libre");
    }

    private static DatosDelNegocio conModulos(Set<String> modulos) {
        return new DatosDelNegocio(UUID.randomUUID(), UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of("ADMINISTRADOR"), modulos, Set.of(), Set.of());
    }
}
