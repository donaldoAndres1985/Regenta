package com.regenta.facturacion.infra;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.aplicacion.ClienteDeLaDian;
import com.regenta.facturacion.aplicacion.DianNoDisponibleException;

/**
 * Stub del cliente de la DIAN. Por defecto acepta. Sus campos configurables
 * dejan a las pruebas —y a una demo— simular rechazo o caída. La integración
 * real (web service DIAN 2.1) reemplaza este bean.
 */
@Component
public class ClienteDeLaDianStub implements ClienteDeLaDian {

    public volatile boolean disponible = true;
    public volatile boolean aceptar = true;
    public volatile String codigoRechazo = "FAD09";
    public volatile String mensajeRechazo = "Documento rechazado por la DIAN";

    public void reiniciar() {
        disponible = true;
        aceptar = true;
        codigoRechazo = "FAD09";
        mensajeRechazo = "Documento rechazado por la DIAN";
    }

    @Override
    public RespuestaDeLaDian transmitir(byte[] xmlFirmado, String ambiente) {
        if (!disponible) {
            throw new DianNoDisponibleException("La DIAN no respondió (" + ambiente + ")");
        }
        String request = new String(xmlFirmado, StandardCharsets.UTF_8);
        if (aceptar) {
            return new RespuestaDeLaDian(true, 200, null, "Documento validado", request,
                    "<Acuse><Estado>00</Estado></Acuse>", 42);
        }
        return new RespuestaDeLaDian(false, 200, codigoRechazo, mensajeRechazo, request,
                "<Acuse><Estado>99</Estado><Codigo>" + codigoRechazo + "</Codigo></Acuse>", 40);
    }
}
