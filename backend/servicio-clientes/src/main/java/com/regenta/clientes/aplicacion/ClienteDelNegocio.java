package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.regenta.clientes.domain.Cliente;

/** La vista de lectura de un cliente. */
public record ClienteDelNegocio(
        UUID id,
        String tipoPersona,
        String tipoDocumento,
        String numeroDocumento,
        String digitoVerificacion,
        String nombres,
        String apellidos,
        String razonSocial,
        String nombreDisplay,
        String email,
        String telefono,
        String telefonoAlterno,
        String segmento,
        boolean creditoHabilitado,
        BigDecimal cupoCredito,
        BigDecimal saldoPendiente,
        boolean activo,
        /** HU-133: con qué régimen se le factura; {@code null} si nadie lo indicó. */
        String regimenFiscal,
        List<String> responsabilidadesFiscales) {

    static ClienteDelNegocio de(Cliente c) {
        return new ClienteDelNegocio(c.getId(), c.getTipoPersona().name(),
                c.getTipoDocumento().name(), c.getNumeroDocumento(), c.getDigitoVerificacion(),
                c.getNombres(), c.getApellidos(), c.getRazonSocial(), c.getNombreDisplay(),
                c.getEmail(), c.getTelefono(), c.getTelefonoAlterno(), c.getSegmento(),
                c.isCreditoHabilitado(), c.getCupoCredito(), c.getSaldoPendiente(), c.isActivo(),
                c.getRegimenFiscal(), c.getResponsabilidadesFiscales());
    }
}
