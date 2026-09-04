package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Los datos con los que salen las facturas y con los que calculan los precios.
 * Mezcla lo que vive en {@code negocios} (razon social, documento) con lo que
 * vive en {@code configuracion_negocio}: para quien configura es una sola
 * pantalla.
 */
public record CambioDeConfiguracion(
        @NotBlank @Size(max = 150) String nombreComercial,
        @Size(max = 200) String razonSocial,
        @NotBlank @Size(max = 10) String tipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @Size(max = 1) String digitoVerificacion,
        @Size(max = 200) String direccion,
        @Size(max = 80) String ciudad,
        @Size(max = 80) String departamento,
        @Size(max = 30) String telefono,
        @Size(max = 150) String email,
        @Size(max = 150) String sitioWeb,
        @Size(max = 15) String codigoPostal,
        String logoUrl,
        @Size(max = 40) String regimenFiscal,
        List<String> responsabilidadesFiscales,
        Short decimalesMoneda,
        @Size(max = 20) String formatoFecha,
        Boolean preciosIncluyenImpuesto,
        Boolean politicaStockNegativo,
        Map<String, Object> preferencias) {
}
