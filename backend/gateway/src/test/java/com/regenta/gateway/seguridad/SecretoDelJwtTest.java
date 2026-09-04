package com.regenta.gateway.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Un gateway que no puede validar firmas no debe arrancar: si arranca, atiende
 * todo lo que le llegue con una firma cualquiera.
 */
class SecretoDelJwtTest {

    @Test
    @DisplayName("Sin secreto el contexto no levanta y el mensaje dice que variable falta")
    void sinSecretoNoArranca() {
        PropiedadesDeSeguridad propiedades = new PropiedadesDeSeguridad();

        assertThatThrownBy(() -> new ConfiguracionDeSeguridad().decodificadorDeJwt(propiedades))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRETO");
    }

    @Test
    @DisplayName("Un secreto corto tampoco pasa: HS256 exige 256 bits")
    void secretoCortoNoArranca() {
        PropiedadesDeSeguridad propiedades = new PropiedadesDeSeguridad();
        propiedades.getJwt().setSecreto("corto");

        assertThatThrownBy(() -> new ConfiguracionDeSeguridad().decodificadorDeJwt(propiedades))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.valueOf(ConfiguracionDeSeguridad.LARGO_MINIMO_DEL_SECRETO));
    }

    @Test
    @DisplayName("Con un secreto suficiente el decodificador se construye")
    void conSecretoSuficienteSeConstruye() {
        PropiedadesDeSeguridad propiedades = new PropiedadesDeSeguridad();
        propiedades.getJwt().setSecreto("secreto-de-pruebas-de-regenta-con-mas-de-32-caracteres");

        Throwable fallo = catchThrowable(() -> new ConfiguracionDeSeguridad().decodificadorDeJwt(propiedades));

        assertThat(fallo).isNull();
    }
}
