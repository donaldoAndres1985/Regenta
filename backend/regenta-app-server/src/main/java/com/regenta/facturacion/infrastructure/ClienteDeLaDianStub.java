package com.regenta.facturacion.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.regenta.facturacion.application.ClienteDeLaDian;
import com.regenta.facturacion.application.DianNoDisponibleException;

/**
 * La DIAN simulada, para desarrollo y demos: se usa solo si no hay {@code DIAN_URL} (HU-117). Por defecto
 * acepta y devuelve el CUFE que trae el XML. Sus campos dejan simular un rechazo o una caída.
 */
@Component
@ConditionalOnExpression("'${regenta.dian.url:}' == ''")
public class ClienteDeLaDianStub implements ClienteDeLaDian {

    private static final Logger LOG = LoggerFactory.getLogger(ClienteDeLaDianStub.class);

    public volatile boolean disponible = true;
    public volatile boolean aceptar = true;
    public volatile String codigoRechazo = "FAD09";
    public volatile String mensajeRechazo = "Documento rechazado por la DIAN";

    public ClienteDeLaDianStub() {
        LOG.warn("Sin DIAN_URL: las facturas van a una DIAN simulada que siempre acepta. Configúrala para "
                + "transmitir de verdad.");
    }

    public void reiniciar() {
        disponible = true;
        aceptar = true;
        codigoRechazo = "FAD09";
        mensajeRechazo = "Documento rechazado por la DIAN";
    }

    @Override
    public RespuestaDeLaDian transmitir(EnvioALaDian envio) {
        if (!disponible) {
            throw new DianNoDisponibleException("La DIAN (simulada) no respondió");
        }
        String request = new String(envio.xmlFirmado(), StandardCharsets.UTF_8);
        String cufe = cufeDe(request);
        if (aceptar) {
            return new RespuestaDeLaDian(true, 200, null, "Documento validado (DIAN simulada)", cufe, List.of(),
                    request, "<Acuse><Estado>00</Estado></Acuse>", 42);
        }
        return new RespuestaDeLaDian(false, 200, "99", mensajeRechazo, null,
                List.of("Regla: " + codigoRechazo + ", Rechazo: " + mensajeRechazo), request,
                "<Acuse><Estado>99</Estado><Codigo>" + codigoRechazo + "</Codigo></Acuse>", 40);
    }

    private static String cufeDe(String xml) {
        int i = xml.indexOf("<cbc:UUID");
        int desde = i < 0 ? -1 : xml.indexOf('>', i);
        int hasta = desde < 0 ? -1 : xml.indexOf("</cbc:UUID>", desde);
        return desde < 0 || hasta < 0 ? null : xml.substring(desde + 1, hasta).trim();
    }
}
