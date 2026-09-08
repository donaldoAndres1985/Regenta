package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.compras.domain.Proveedor;

public record ProveedorDelNegocio(
        UUID id,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String nombreComercial,
        String contactoNombre,
        String email,
        String telefono,
        String ciudad,
        int diasCredito,
        BigDecimal cupoCredito,
        BigDecimal saldoPendiente,
        Integer calificacion,
        boolean activo) {

    static ProveedorDelNegocio de(Proveedor p) {
        return new ProveedorDelNegocio(p.getId(), p.getTipoDocumento().name(),
                p.getNumeroDocumento(), p.getRazonSocial(), p.getNombreComercial(),
                p.getContactoNombre(), p.getEmail(), p.getTelefono(), p.getCiudad(),
                p.getDiasCredito(), p.getCupoCredito(), p.getSaldoPendiente(),
                p.getCalificacion() == null ? null : (int) (short) p.getCalificacion(),
                p.isActivo());
    }
}
