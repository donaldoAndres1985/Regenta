package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.compras.domain.Proveedor;

/**
 * Un proveedor como lo ve la app (HU-046, HU-166).
 *
 * <p>{@code deuda} se calcula con las cuentas por pagar abiertas (I5 de {@code docs/dominio/compras.md});
 * no se lee de {@code proveedores.saldo_pendiente}, que nunca se mantuvo. {@code superaCupo}
 * solo informa (I6): el cupo lo controla el proveedor, no Regenta.
 */
public record ProveedorDelNegocio(
        UUID id,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String nombreComercial,
        String contactoNombre,
        String email,
        String telefono,
        String direccion,
        String ciudad,
        int diasCredito,
        BigDecimal cupoCredito,
        BigDecimal deuda,
        boolean superaCupo,
        Integer calificacion,
        String notas,
        boolean activo) {

    static ProveedorDelNegocio de(Proveedor p, BigDecimal deuda) {
        BigDecimal cupo = p.getCupoCredito();
        boolean supera = cupo != null && cupo.signum() > 0 && deuda.compareTo(cupo) > 0;
        return new ProveedorDelNegocio(p.getId(), p.getTipoDocumento().name(),
                p.getNumeroDocumento(), p.getRazonSocial(), p.getNombreComercial(),
                p.getContactoNombre(), p.getEmail(), p.getTelefono(), p.getDireccion(),
                p.getCiudad(), p.getDiasCredito(), cupo, deuda, supera,
                p.getCalificacion() == null ? null : (int) (short) p.getCalificacion(),
                p.getNotas(), p.isActivo());
    }
}
