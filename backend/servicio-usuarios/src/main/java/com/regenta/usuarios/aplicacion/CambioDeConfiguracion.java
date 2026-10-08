package com.regenta.usuarios.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
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
        Map<String, Object> preferencias,
        /**
         * HU-137: sobre este total la venta no se cobra a consumidor final.
         * Como los datos fiscales, se manda siempre: {@code null} es "no se
         * exige", no "dejar el que había".
         */
        @DecimalMin("0") BigDecimal montoIdentificarComprador) {

    /** La configuración de antes de HU-137, sin monto para identificar al comprador. */
    public CambioDeConfiguracion(String nombreComercial, String razonSocial, String tipoDocumento,
            String numeroDocumento, String digitoVerificacion, String direccion, String ciudad,
            String departamento, String telefono, String email, String sitioWeb,
            String codigoPostal, String logoUrl, String regimenFiscal,
            List<String> responsabilidadesFiscales, Short decimalesMoneda, String formatoFecha,
            Boolean preciosIncluyenImpuesto, Boolean politicaStockNegativo,
            Map<String, Object> preferencias) {
        this(nombreComercial, razonSocial, tipoDocumento, numeroDocumento, digitoVerificacion,
                direccion, ciudad, departamento, telefono, email, sitioWeb, codigoPostal, logoUrl,
                regimenFiscal, responsabilidadesFiscales, decimalesMoneda, formatoFecha,
                preciosIncluyenImpuesto, politicaStockNegativo, preferencias, null);
    }
}
