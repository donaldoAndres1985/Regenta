package com.regenta.usuarios.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.shared.correo.RemitenteDelNegocio;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;
import com.regenta.usuarios.api.ConfiguracionNegocioApi.DatosNegocioTicket;

/**
 * HU-122 · El correo de un negocio sale con su nombre comercial (o su razón social) y se le responde a
 * su correo de contacto: los dos salen de la configuración del negocio, no de una constante.
 */
@Component
public class RemitenteDesdeLaConfiguracion implements RemitenteDelNegocio {

    private final ConfiguracionNegocioApi configuracion;

    public RemitenteDesdeLaConfiguracion(ConfiguracionNegocioApi configuracion) {
        this.configuracion = configuracion;
    }

    @Override
    public Remitente de(UUID negocioId) {
        DatosNegocioTicket d = configuracion.datosTicket(negocioId);
        String nombre = !vacio(d.nombreComercial()) ? d.nombreComercial()
                : !vacio(d.razonSocial()) ? d.razonSocial() : "Regenta";
        return new Remitente(nombre, vacio(d.email()) ? null : d.email().trim());
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
