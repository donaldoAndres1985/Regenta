package com.regenta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTests {

    @Test
    @DisplayName("Verifica la integridad modular de Regenta con Spring Modulith")
    void modularidadValida() {
        ApplicationModules modulos = ApplicationModules.of(RegentaApplication.class);
        modulos.verify();
    }

    @Test
    @DisplayName("Genera la documentación y diagramas de arquitectura con Spring Modulith")
    void generarDocumentacion() {
        ApplicationModules modulos = ApplicationModules.of(RegentaApplication.class);
        new Documenter(modulos)
                .writeDocumentation()
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
