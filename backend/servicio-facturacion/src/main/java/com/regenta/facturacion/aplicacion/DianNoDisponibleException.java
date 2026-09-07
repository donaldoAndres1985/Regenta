package com.regenta.facturacion.aplicacion;

/**
 * La DIAN (o el proveedor tecnológico) no respondió: un fallo de red o de
 * disponibilidad, no un rechazo. La factura no se pierde; se reintenta.
 */
public class DianNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DianNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public DianNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
