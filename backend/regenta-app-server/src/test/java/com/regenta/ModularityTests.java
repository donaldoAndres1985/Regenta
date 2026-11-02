package com.regenta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    @DisplayName("Verifica la integridad modular de Regenta con Spring Modulith")
    void modularidadValida() {
        ApplicationModules modulos = ApplicationModules.of(RegentaApplication.class);
        modulos.verify();
    }
}
