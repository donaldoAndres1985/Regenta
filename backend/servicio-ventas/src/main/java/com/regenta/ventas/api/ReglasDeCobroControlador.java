package com.regenta.ventas.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.ventas.aplicacion.ConfiguracionDelNegocio;
import com.regenta.ventas.aplicacion.ReglasDeCobro;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Lo que el POS necesita saber antes de cobrar. HU-137. */
@RestController
@RequestMapping("/api/ventas/reglas-de-cobro")
@Tag(name = "Reglas de cobro", description = "Lo que el negocio exige antes de cobrar una venta")
public class ReglasDeCobroControlador {

    private final ConfiguracionDelNegocio configuracion;

    public ReglasDeCobroControlador(ConfiguracionDelNegocio configuracion) {
        this.configuracion = configuracion;
    }

    @GetMapping
    @Operation(summary = "El monto desde el cual hay que identificar al comprador (null si no se exige)")
    public ReglasDeCobro reglas() {
        return configuracion.reglasDeCobro();
    }
}
