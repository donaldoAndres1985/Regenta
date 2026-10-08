package com.regenta.facturacion.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Con qué se factura cuando quien compra no se identifica
 * (`design/comportamiento/ClienteVenta.md`, R8).
 *
 * <p>{@code facturas.cliente_snapshot} es NOT NULL y la mayoría de las ventas
 * de mostrador van sin cliente. Emitir con el adquiriente vacío no es una
 * factura incompleta: es una factura que la DIAN rechaza.
 *
 * <p>No es un cliente y no se guarda en {@code crm.clientes}: es lo que el
 * anexo técnico manda escribir en el XML. Los valores están en configuración y
 * no en el código porque el día que la DIAN cambie el número —o que haya que
 * corregirlo— tiene que ser un cambio de configuración, no un despliegue con
 * migración.
 *
 * <p><b>Sin confirmar:</b> la cantidad de doses del número. Aquí van doce, que
 * es lo que trae el anexo técnico de factura electrónica; los nueve son los del
 * mundo POS y de los sistemas anteriores. Confirmar contra el anexo vigente
 * antes de la primera emisión real.
 */
@Component
public class AdquirienteGenerico {

    private final Map<String, Object> snapshot;
    private final String regimenPorDefecto;
    private final String responsabilidadPorDefecto;

    public AdquirienteGenerico(
            @Value("${regenta.dian.adquiriente-generico.nombre:Consumidor final}") String nombre,
            @Value("${regenta.dian.adquiriente-generico.tipo-documento:13}") String tipoDocumento,
            @Value("${regenta.dian.adquiriente-generico.numero-documento:222222222222}")
            String numeroDocumento,
            @Value("${regenta.dian.adquiriente-generico.tipo-organizacion:2}") String tipoOrganizacion,
            @Value("${regenta.dian.adquiriente-generico.responsabilidad-fiscal:R-99-PN}")
            String responsabilidadFiscal,
            @Value("${regenta.dian.adquiriente-generico.pais:CO}") String pais,
            @Value("${regenta.dian.regimen-por-defecto.regimen-fiscal:NO_RESPONSABLE}")
            String regimenPorDefecto,
            @Value("${regenta.dian.regimen-por-defecto.responsabilidad-fiscal:R-99-PN}")
            String responsabilidadPorDefecto) {
        this.regimenPorDefecto = regimenPorDefecto;
        this.responsabilidadPorDefecto = responsabilidadPorDefecto;
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("tipo_documento", tipoDocumento);
        datos.put("numero_documento", numeroDocumento);
        datos.put("tipo_organizacion", tipoOrganizacion);
        datos.put("responsabilidad_fiscal", responsabilidadFiscal);
        datos.put("pais", pais);
        datos.put("generico", true);
        this.snapshot = Map.copyOf(datos);
    }

    /**
     * Una copia por factura: el snapshot se guarda como JSONB y queda
     * congelado con el documento. Si mañana cambia la configuración, las
     * facturas ya emitidas siguen diciendo lo que decían.
     */
    public Map<String, Object> snapshot() {
        return new LinkedHashMap<>(snapshot);
    }

    /**
     * HU-133 criterio 4: un cliente identificado pero sin régimen en el CRM
     * sale con el de persona natural, y el snapshot dice que fue por defecto
     * —quien revise un rechazo de la DIAN tiene que poder distinguir "el
     * cliente es no responsable" de "nadie lo indicó".
     */
    public Map<String, Object> conRegimen(Map<String, Object> cliente) {
        Object regimen = cliente.get("regimen_fiscal");
        if (regimen != null && !regimen.toString().isBlank()) {
            return cliente;
        }
        Map<String, Object> completo = new LinkedHashMap<>(cliente);
        completo.put("regimen_fiscal", regimenPorDefecto);
        completo.put("responsabilidades_fiscales", List.of(responsabilidadPorDefecto));
        completo.put("regimen_por_defecto", true);
        return completo;
    }
}
