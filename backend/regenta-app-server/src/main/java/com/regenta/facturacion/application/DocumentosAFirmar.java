package com.regenta.facturacion.application;

import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.infrastructure.FacturaImpuestoRepositorio;
import com.regenta.facturacion.infrastructure.FacturaLineaRepositorio;
import com.regenta.facturacion.infrastructure.ResolucionRepositorio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-117 · Junta lo que necesita el XML de una factura ya emitida: líneas, impuestos y resolución. */
@Component
public class DocumentosAFirmar {

    private final FacturaLineaRepositorio lineas;
    private final FacturaImpuestoRepositorio impuestos;
    private final ResolucionRepositorio resoluciones;
    private final ParametrosDeLaDian parametros;

    public DocumentosAFirmar(FacturaLineaRepositorio lineas, FacturaImpuestoRepositorio impuestos,
            ResolucionRepositorio resoluciones, ParametrosDeLaDian parametros) {
        this.lineas = lineas;
        this.impuestos = impuestos;
        this.resoluciones = resoluciones;
        this.parametros = parametros;
    }

    public DocumentoAFirmar de(Factura factura) {
        Resolucion resolucion = resoluciones.findByIdAndNegocioId(factura.getResolucionId(), factura.getNegocioId())
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "La resolución de la factura " + factura.getNumeroCompleto() + " no existe"));
        return new DocumentoAFirmar(factura, lineas.findByFacturaIdOrderByLineaAsc(factura.getId()),
                impuestos.findByFacturaId(factura.getId()), resolucion);
    }

    /** El nombre base de sus archivos ante la DIAN (anexo 6.5.7). */
    public String nombreDeArchivo(Factura factura) {
        Object nit = factura.getEmisorSnapshot() == null ? null : factura.getEmisorSnapshot().get("numero_documento");
        var fecha = factura.getFechaEmision() == null ? java.time.LocalDate.now()
                : factura.getFechaEmision().withOffsetSameInstant(ZoneOffset.ofHours(-5)).toLocalDate();
        return parametros.nombreDeArchivo(nit == null ? "" : nit.toString(), fecha, factura.getNumero());
    }
}
