package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La configuracion tal como queda. La advertencia no es decoracion: cambiar si
 * los precios incluyen impuesto cambia como se calcula toda venta nueva.
 */
public record ConfiguracionVigente(
        UUID negocioId,
        String nombreComercial,
        String razonSocial,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        String direccion,
        String ciudad,
        String departamento,
        String telefono,
        String email,
        String sitioWeb,
        String codigoPostal,
        String logoUrl,
        String regimenFiscal,
        List<String> responsabilidadesFiscales,
        short decimalesMoneda,
        String formatoFecha,
        boolean preciosIncluyenImpuesto,
        boolean politicaStockNegativo,
        UUID impuestoPorDefecto,
        Map<String, Object> preferencias,
        String advertencia) {
}
