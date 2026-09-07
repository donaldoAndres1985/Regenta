package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.facturacion.domain.Resolucion;

/** La vista de lectura de una resolución. */
public record ResolucionDelNegocio(
        UUID id,
        UUID sucursalId,
        String tipoDocumento,
        String numeroResolucion,
        String prefijo,
        long rangoDesde,
        long rangoHasta,
        long consecutivoActual,
        long numerosDisponibles,
        boolean porAgotarse,
        String claveTecnica,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String ambiente,
        String estado) {

    static ResolucionDelNegocio de(Resolucion r) {
        return new ResolucionDelNegocio(r.getId(), r.getSucursalId(),
                r.getTipoDocumento().name(), r.getNumeroResolucion(), r.getPrefijo(),
                r.getRangoDesde(), r.getRangoHasta(), r.getConsecutivoActual(),
                r.numerosDisponibles(), r.porDebajoDelUmbral(), r.getClaveTecnica(),
                r.getVigenteDesde(), r.getVigenteHasta(), r.getAmbiente().name(),
                r.getEstado().name());
    }
}
