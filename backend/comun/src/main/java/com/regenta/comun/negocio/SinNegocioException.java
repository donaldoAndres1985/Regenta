package com.regenta.comun.negocio;

/** No hay negocio en contexto: la peticion no paso por el gateway o el token no lo traia. */
public class SinNegocioException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public SinNegocioException(String mensaje) {
        super(mensaje);
    }
}
