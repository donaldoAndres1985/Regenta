package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.facturacion.domain.Certificado;

public record CertificadoDelNegocio(
        UUID id,
        String alias,
        String emisor,
        String numeroSerie,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String referenciaKms,
        String estado,
        boolean porVencer) {

    static CertificadoDelNegocio de(Certificado c, LocalDate hoy) {
        return new CertificadoDelNegocio(c.getId(), c.getAlias(), c.getEmisor(), c.getNumeroSerie(),
                c.getVigenteDesde(), c.getVigenteHasta(), c.getReferenciaKms(),
                c.getEstado().name(), c.porVencer(hoy, 30));
    }
}
